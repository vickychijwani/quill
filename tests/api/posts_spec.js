var _ = require('lodash-node');
var api = require('./api');
var types = require('./types');
var postModel = require('./post');

// common bits of data
var postEmpty = {
    title: '(Untitled)',
    status: 'draft',
    markdown: '',
    tags: []
};
var postEmptyResp = _.merge({}, postEmpty, {
    slug: function (val) { return expect(val).toMatch(/^untitled/); },
    html: '',
});

var postNonEmpty = {
    title: 'Test post title',
    status: 'draft',
    markdown: 'Test *post* __body__!',
    tags: [{
       name: 'test-tag'
    }],
};
var postNonEmptyResp = _.merge({}, postNonEmpty, {
    html: '<p>Test <em>post</em> <strong>body</strong>!</p>'
});

// trigger the test
startTest();

// test definitions
function startTest() {
    api.delete('Posts', '/db/')
        .expectStatus(200)
        .after(createEmptyPost)
        .run();
}

function createEmptyPost() {
    api.post('Post create empty', '/posts/?include=tags',
        { posts: [postEmpty] })
        .expectStatus(201)
        .expectJSONTypes('posts.*', postModel.schema)
        .expectJSON('posts.?', postEmptyResp)
        .after(createNonEmptyPost)
        .run();
}

function createNonEmptyPost() {
    api.post('Post create non-empty', '/posts/?include=tags', { posts: [postNonEmpty] })
        .expectStatus(201)
        .expectJSONTypes('posts.*', postModel.schema)
        .expectJSON('posts.?', postNonEmptyResp)
        .afterJSON(updatePostTitle)
        .run();
}

function updatePostTitle(json) {
    var edit = { title: 'Test post title updated!' };
    _.merge(postNonEmpty, edit);
    _.merge(postNonEmptyResp, edit);
    api.put('Post update title', '/posts/' + json.posts[0].id + '/?include=tags',
        { posts: [postNonEmpty] })
        .expectJSON('posts.?', postNonEmptyResp)
        .after(testPostList)
        .run();
}

function testPostList() {
    api.get('Post list', '/posts/?status=all&staticPages=all&limit=all&include=tags')
        .expectStatus(200)
        .expectJSONTypes('posts.*', postModel.schema)
        .expectJSON('posts.?', postEmptyResp)
        .expectJSON('posts.?', postNonEmptyResp)
        .after(testIfNoneMatch)
        .run();
}

function testIfNoneMatch(err, response, body) {
    api.get('Post list (from cache)', '/posts/?status=all&staticPages=all&limit=all&include=tags')
        .addHeader('If-None-Match', response.headers.etag)
        .expectStatus(304)
        .run();
}
