module.exports = {
    StringOrNull: function (val) { return expect(val).toBeTypeOrNull(String); },
    NumberOrNull: function (val) { return expect(val).toBeTypeOrNull(Number); },
    DateString: function (val) { return expect(val).toMatch(/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d\d\dZ$/); },
    DateStringOrNull: function (val) { return expect(val).toMatchOrBeNull(/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d\d\dZ$/); },
};
