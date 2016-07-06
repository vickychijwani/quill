[![](https://dl.dropboxusercontent.com/u/3037831/quill-github/feature-graphic.png)][playstore]

<img src="screenshots/demo.gif" width="320" align="right" hspace="20">

[![](https://img.shields.io/circleci/project/vickychijwani/quill.svg)](https://circleci.com/gh/vickychijwani/quill)
[![Join the chat at https://gitter.im/vickychijwani/quill](https://badges.gitter.im/vickychijwani/quill.svg)](https://gitter.im/vickychijwani/quill?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Quill-green.svg?style=true)](https://android-arsenal.com/details/3/3729)
![](https://img.shields.io/github/tag/vickychijwani/quill.svg)
[![](https://img.shields.io/badge/license-MIT-blue.svg)](/LICENSE)

Quill is the beautiful Android app for your [Ghost](https://ghost.org) blog. Get it [here on the Google Play Store][playstore].

<a href='https://play.google.com/store/apps/details?id=me.vickychijwani.spectre&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width='300px'/></a>

### Features

- Simple, intuitive interface based on **[Material Design](https://material.google.com/)** principles (along with some **tasteful animations**!)
- **Markdown editing with preview** - swipe to switch between editing and previewing
- **100% Markdown compatibility with Ghost** - go ahead and use footnotes and code blocks like you're used to
- Works with **[Ghost Pro](https://ghost.org/pricing/)** (ghost.io) as well as **self-hosted blogs**
- **Offline mode**: Quill is designed to work 100% offline: just sync when you're connected later! Ideal for writing on the go
- **Attach tags and a cover image** - upload images from your phone or a web link

### Bug reports? Feature requests?

[File an issue](/CONTRIBUTING.md)



### Developer setup

Quill uses [Fabric](http://fabric.io/) (formerly Crashlytics) for automatic crash-reporting. However my Fabric API key and secret is not committed to this Github repo for security reasons. So to build the app in Android Studio, you will have to either:

- Create the file `app/crashlytics.properties` and set your own Fabric API key and secret like this:

```
apiKey=YOUR_API_KEY_HERE
apiSecret=YOUR_API_SECRET_HERE
```

- OR, to skip Crashlytics altogether, create an empty file `app/crashlytics.properties` and comment out the call `Fabric.with(this, new Crashlytics())` in `SpectreApplication.java` line 51

If you face any issues setting this up, please let me know by [filing a new issue](/issues/new).

### Contributors

- [@vickychijwani](https://github.com/vickychijwani)
- [@dexafree](https://github.com/dexafree) (Spanish translation)
- [@Dennis-Mayk](https://github.com/Dennis-Mayk) (German translation)


[playstore]: https://play.google.com/store/apps/details?id=me.vickychijwani.spectre
