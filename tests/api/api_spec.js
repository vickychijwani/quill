var _ = require('lodash-node');
var api = require('./api');
var types = require('./types');
var postModel = require('./post');
var tokenModel = require('./auth_token');

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

// common functions
function deleteAllPosts(afterCallback) {
    var deleteApiCall = api.delete('Delete all posts and tags', '/db/')
        .expectStatus(200);
    if (typeof afterCallback !== 'undefined' && afterCallback !== null) {
        deleteApiCall.after(afterCallback);
    }
    deleteApiCall.run();
}

// test definitions
function startTest() {
    startLoginTest();
}

function startLoginTest() {
    loginWithPassword();
}

function loginWithPassword() {
    api.post('Login with password', '/authentication/token', {
                client_id: 'ghost-admin',
                grant_type: 'password',
                username: 'vickychijwani@gmail.com',
                password: 'ghosttest'
            }, false)
        .expectStatus(200)
        .expectJSONTypes(_.merge({}, tokenModel.schema, { refresh_token: String }))
        .afterJSON(loginWithRefreshToken)
        .run();
}

function loginWithRefreshToken(originalTokens) {
    // revoke the current access token before renewing it
    revokeAccessToken(originalTokens, false);

    api.post('Login with refresh token', '/authentication/token', {
                client_id: 'ghost-admin',
                grant_type: 'refresh_token',
                refresh_token: originalTokens.refresh_token
            }, false)
        .expectStatus(200)
        .expectJSONTypes(tokenModel.schema)
        .afterJSON(function (newTokens) {
            revokeAccessToken(_.merge({}, originalTokens, newTokens), true);
        })
        .run();
}

function revokeAccessToken(tokens, shouldRevokeRefreshToken) {
    api.post('Revoke access token', '/authentication/revoke', {
                client_id: 'ghost-admin',
                token_type_hint: 'access_token',
                token: tokens.access_token
            }, false)
        .expectStatus(200)
        .afterJSON(function (json) {
            // API returns status 200 even in case of errors!
            if (typeof json.error !== 'undefined') {
                throw new Error(json.error);
            }
            if (shouldRevokeRefreshToken) {
                revokeRefreshToken(tokens);
            }
        })
        .run();
}

function revokeRefreshToken(tokens) {
    api.post('Revoke refresh token', '/authentication/revoke', {
                client_id: 'ghost-admin',
                token_type_hint: 'refresh_token',
                token: tokens.refresh_token
            }, false)
        .expectStatus(200)
        .afterJSON(function (json) {
            if (typeof json.error !== 'undefined') {
                throw new Error(json.error);
            }
            revokeAccessTokenError();
        })
        .run();
}

function revokeAccessTokenError() {
    api.post('Revoke access token with error', '/authentication/revoke', {
                client_id: 'ghost-admin',
                token_type_hint: 'access_token',
                token: null     // invalid token, should get error
            }, false)
        .expectStatus(200)
        .afterJSON(function (json) {
            // API returns status 200 even in case of errors!
            if (typeof json.error === 'undefined') {
                throw new Error('API did not return error!');
            }
            endLoginTest();
        })
        .run();
}

function endLoginTest() {
    startPostsTest();
}

function startPostsTest() {
    deleteAllPosts(createEmptyPost);
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
        .after(endPostsTest)
        .run();
}

function endPostsTest() {
    deleteAllPosts();
}

// trigger the test
startTest();
