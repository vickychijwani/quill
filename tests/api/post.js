var _ = require('lodash-node');
var types = require('./types');
var tagModel = require('./tag');

module.exports = {};

module.exports.schema = {
    uuid: String,
    id: Number,
    title: String,
    slug: String,
    status: function (val) { expect(val).toMatch(/^(draft|published)$/) },
    markdown: String,
    html: String,
    tags: function (tags) {
        return _.all(tags, function (t) {
            return expect(t).toContainJsonTypes(tagModel.schema);
        });
    },
    image: types.StringOrNull,
    featured: Boolean,
    page: Boolean,
    language: 'en_US',
    author: Number,

    created_by: Number,
    updated_by: Number,
    published_by: types.NumberOrNull,
    created_at: types.DateString,
    updated_at: types.DateString,
    published_at: types.DateStringOrNull,
    meta_title: types.StringOrNull,
    meta_description: types.StringOrNull,
};
