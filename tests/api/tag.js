var types = require('./types');

module.exports = {};

module.exports.schema = {
    uuid: String,
    name: String,

    slug: String,
    description: types.StringOrNull,
    image: types.StringOrNull,
    hidden: Boolean,

    meta_title: types.StringOrNull,
    meta_description: types.StringOrNull,

    created_at: types.DateString,
    updated_at: types.DateString,
};
