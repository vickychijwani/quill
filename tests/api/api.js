var frisby = require('frisby');

module.exports = {
    get: function (name, endpoint) {
        return createTest(name, 'get', endpoint);
    },
    post: function (name, endpoint, params, useJson) {
        return createTest(name, 'post', endpoint, params, useJson);
    },
    put: function (name, endpoint, params, useJson) {
        return createTest(name, 'put', endpoint, params, useJson);
    },
    delete: function (name, endpoint) {
        return createTest(name, 'delete', endpoint);
    }
}

var baseUrl = 'http://localhost:1234/ghost/api/v0.1';
var token = 'JMsAEmgGSHLRosvDgW2f8qO3xtKQ7w0QB5eInG5PwvhZV8awahR4gajhuPFoP1saSB4ShFUOma2D8rdFzvrnhQAtxuPCOuz1ipUZMxCYyDTlVodtJR7JAkMw6LxvJVs5FLEmxlw5sdwBvM8W96QmHtiBlDACaA9cSnVLZM1oq73lHOjUFI74XSFLHxcJsMZLeR3UfSXP09g8CySl7ExB7XMPAyPbkzy0G6rpBQYNK7GfQ29e4bkqNCoao6AhQLF';

function createTest(name, method, endpoint, params, useJson) {
    var test = frisby.create(name)
        [method](baseUrl + endpoint, params, { json: (useJson !== false) })
        .addHeader('Authorization', 'Bearer ' + token)
    test.run = test.toss;
    delete test.toss;
    return test;
}
