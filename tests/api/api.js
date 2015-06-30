var frisby = require('frisby');

module.exports = {
    get: function (name, endpoint) {
        return createTest(name, 'get', endpoint);
    },
    post: function (name, endpoint, params) {
        return createTest(name, 'post', endpoint, params);
    },
    put: function (name, endpoint, params) {
        return createTest(name, 'put', endpoint, params);
    },
    delete: function (name, endpoint) {
        return createTest(name, 'delete', endpoint);
    }
}

var baseUrl = 'http://localhost:1234/ghost/api/v0.1';
var token = '9kM0jdAUH0HnMLYK8kaDCY4UDdXpvFBcNquVZ4mPVmmF73Kqw2Y9auDiz6A7uO1i99MP6qiozbgBoH0cXenMyJKhawjvjncnyXhyaKZjtSK8G2VJ5ElFNWepshC05FhwjlnRwmrl0JYoUNk8pqxBwQ6tSzNNIOfVUttKTaliBdObfArLh2qtRE8cysRDaCYhpQJeqCODl5x39IlnttkixYc4GLin1lRFQDgMSuSLaRgCCAaphDvMySpM9rtDaXx';

function createTest(name, method, endpoint, params) {
    var test = frisby.create(name)
        [method](baseUrl + endpoint, params, { json: true })
        .addHeader('Authorization', 'Bearer ' + token)
    test.run = test.toss;
    delete test.toss;
    return test;
}
