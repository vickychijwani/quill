<img src="screenshots/demo.gif" width="320" align="right" hspace="20">

[![](https://img.shields.io/circleci/project/vickychijwani/quill.svg)](https://circleci.com/gh/vickychijwani/quill)
[![Translate on Weblate](https://hosted.weblate.org/widgets/ghost/-/svg-badge.svg)](https://hosted.weblate.org/engage/ghost/en/?utm_source=widget)
![](https://img.shields.io/github/tag/vickychijwani/quill.svg)

### THIS REPO HAS MOVED HOUSE :truck:: It now lives at [TryGhost/Ghost-Android](https://github.com/TryGhost/Ghost-Android) since Quill became the official Android app for Ghost! See https://blog.ghost.org/android/.

Quill is the beautiful Android app for your [Ghost](https://ghost.org) blog. Get it [here on the Google Play Store][playstore].

<a href='https://play.google.com/store/apps/details?id=org.ghost.android&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width='300px'/></a>

### Features

- Simple, intuitive interface based on **[Material Design](https://material.google.com/)** principles (along with some **tasteful animations**!)
- **Markdown editing with preview** - swipe to switch between editing and previewing
- **100% Markdown compatibility with Ghost** - go ahead and use footnotes and code blocks like you're used to
- Works with **[Ghost Pro](https://ghost.org/pricing/)** (ghost.io) as well as **self-hosted blogs**
- **Offline mode**: Quill is designed to work 100% offline: just sync when you're connected later! Ideal for writing on the go
- **Attach tags and a cover image** - upload images from your phone or a web link
- **[Conflict handling][conflict-ui]** - when a blog post is edited simultaneously from the app and elsewhere (e.g., via Ghost web), Quill will detect the conflict and show you a nice UI to decide what to do - so you never lose your hard work!

### Bug reports? Feature requests?

[File an issue](/CONTRIBUTING.md)

### Help translate Quill

If you'd like to see support for your language in Quill, you can easily [contribute translations on Weblate][weblate]. _This requires no setup and no knowledge of Android development_, just keep in mind a few simple rules:

- Many strings have [placeholders](http://envyandroid.com/android-string-xml-resource-formatting/) like "%s", "%d", "%2$s", etc. - keep these intact because other numbers and strings are inserted into these placeholders
- Single and double quotes need to be preceded with a backslash character (\' and \" respectively)
- For short strings, try to keep the translated string length close to the English one, because longer strings may not fit in the UI

If you need help getting started, [email me](mailto:vickychijwani@gmail.com) or drop a comment on the [master translation issue](https://github.com/vickychijwani/quill/issues/26).

### Developer setup

Setup is as simple as importing the project into Android Studio and building (assuming you have the correct build tools and Android SDK).

If you face any issues setting this up, please let me know by [filing a new issue](/issues/new).

### Contributors

- [@vickychijwani](https://github.com/vickychijwani)
- [@dexafree](https://github.com/dexafree) (Spanish translation)
- [@Dennis-Mayk](https://github.com/Dennis-Mayk) (German translation)
- [@naofum](https://github.com/naofum) (Japanese translation)
- [@svenkapudija](https://github.com/svenkapudija) (Croatian translation)
- [@yffengdong](https://hosted.weblate.org/user/yffengdong/) (Chinese zh-CN translation)
- [@guillaumevidal](https://github.com/guillaumevidal) (French translation)
- [@fastbyte01](https://hosted.weblate.org/user/fastbyte01/) (Italian translation)
- [@cristears](https://hosted.weblate.org/user/cristears/) (Korean translation)
- [Allan Nordhøy](https://hosted.weblate.org/user/kingu/) (Norwegian translation)


[playstore]: https://play.google.com/store/apps/details?id=org.ghost.android
[weblate]: https://hosted.weblate.org/engage/ghost/en/
[conflict-ui]: https://github.com/vickychijwani/quill/issues/144#issuecomment-264991612
