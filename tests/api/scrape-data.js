var _ = require('lodash-node');
var xray = require('x-ray')();
var request = require('request-json');

var api = request.createClient('http://vickychijwani.me:2369/ghost/api/v0.1/');
api.headers['Authorization'] = 'Bearer J0Imbg8vELt73bUsgXyyeWJtSKoPyTIaIOsJl4ZgQEgNClTYNFQl7VwkU3BFybbaABBzctBF6dGgvyDPxsRLMYKufsoTGInyUmLQN9ho6y10LJxudLzsxWf7sSOg1z9rnHnw37b3PNLwTrfCU6aLfkAmqdKUE7SKUK5POKRRu8gW28LsjA9ixaULo7Bg7oS5bB66tDZoymydnUXyQwQqtG4piJwpbL874kn8c69ALww0SpO5UBZGvjWFCMldgZd';

var NUM_ARTICLES = 100;
var BATCH_UPLOAD_SIZE = 1;    // Ghost's API currently does not support uploading more than 1 post at a time :(

// firstly, delete all posts
api.del('db', function (err, res) {
  if (res.statusCode === 200) {
    console.log('All posts deleted\n');
    scrape();
  } else {
    throw new Error('Could not delete posts, status = ' + res.statusCode);
  }
});

var postLinks = '';
function scrape () {
  var postLinksStream = xray('http://www.nomadicmatt.com/travel-blog/', 'h2.entry-title', [{
    link: 'a@href'
  }])
      .paginate('.pagination-next a@href')
      .limit(20)
      .write();

  postLinks = '';
  postLinksStream.on('data', function (c) { postLinks += c; });

  postLinksStream.on('end', function () {
    postLinks = _(JSON.parse(postLinks)).pluck('link').take(NUM_ARTICLES).value();
    console.log('Got ' + postLinks.length + ' links:');
    postLinks.forEach(function (link) {
      console.log('    ' + link);
    });
    console.log('');
    scrapeAndUpload();
  });
}

var numUploaded = 0;
function scrapeAndUpload() {
  if (postLinks.length === 0) {
    // ... AAAAND we're done
    console.log('Total uploaded: ' + numUploaded + ' posts! :)');
    return;
  }
  var articles = [];
  _.take(postLinks, BATCH_UPLOAD_SIZE).forEach(function (link) {
    console.log('Scraping ' + link);
    var stream2 = xray(link, 'article', {
      title: 'h1.entry-title',
      image: '.entry-content img@src',
      markdown: '.entry-content@html',
    }).write();

    var article = '';
    stream2.on('data', function (c) { article += c; });
    stream2.on('end', function () {
      article = JSON.parse(article);
      article.status = 'published';
      articles.push(article);
      if (articles.length < BATCH_UPLOAD_SIZE) {
        return;  // don't upload yet...
      }
      uploadArticles(articles);
    });
  });

  if (articles.length > 0) {
    uploadArticles(articles);
  }
}

function uploadArticles(articles) {
  console.log('Batch uploading ' + articles.length + ' articles:');
  _.pluck(articles, 'title').forEach(function (title) {
    console.log('    ' + title);
  });
  api.post('posts?include=tags', {posts: articles}, function (err, res) {
    if (res.statusCode === 201) {
      console.log('Posts uploaded!\n');
      numUploaded = numUploaded + articles.length;
    } else if (res.statusCode === 401) {
      throw new Error('Got error 401, change the access token!');
    } else {
      console.log('Could not upload posts, status = ' + res.statusCode + '\n');
    }
    articles.length = 0;                      // clear the array
    postLinks.splice(0, BATCH_UPLOAD_SIZE);   // delete the uploaded posts
    scrapeAndUpload();                        // continue scraping
  });
}
