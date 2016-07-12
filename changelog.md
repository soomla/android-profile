### v1.2.3 [view commit logs](https://github.com/soomla/android-profile/compare/v1.2.2...v1.2.3)

* Changes
  * Updated Google play services to 8.4

### v1.2.2 [view commit logs](https://github.com/soomla/android-profile/compare/v1.2.1...v1.2.2)

* Changes
  * Allow SoomlaProfile to work without providers

### v1.2.1 [view commit logs](https://github.com/soomla/android-profile/compare/v1.2.0...v1.2.1)

* Changes
  * Removed binaries from repo

### v1.2.0 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.9...v1.2.0)

* New Features
  * Added Google-Play Game Services social provider - with Leaderboards

* Changes
  * Changed Provider inheritance structure

### v1.1.9 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.8...v1.1.9)

* Changes
  * Added check for getCurrentPerson in G+ login

### v1.1.8 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.7...v1.1.8)

* Changes
  * Changed `SoomlaProfile.isLoggedIn()` method to work without `Activity`

* Features
  * Added new method `SoomlaProfile.logoutFromAllProviders()`

### v1.1.7 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.6...v1.1.7)

* Changes
  * Updated SimpleFacebook library to v4.0.9

### v1.1.6 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.5...v1.1.6)

* Features 
  * Added new functionality to Profile

### v1.1.5 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.4...v1.1.5)

* Fixes
  * Calling SoomlaProfile.login from within OnSoomlaProfileInitialized triggers a CC_ASSERT(mInited)
  * Make all implementations safe for null param
  * After loggin out Twitter logging in with the same profile instead of showing login form

* Changes
  * Removed binaries and improved build scripts
  * Aligned UserProfile implementation with other platforms

### v1.1.4 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.3...v1.1.4)

* Changes
  * Improved error log message

* Fixes
  * Minor issues in extra params

### v1.1.3 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.2...v1.1.3)

* New Features
  * Support extra params in UserProfile

* Changes
  * Added version to code

* Fixes
  * Add "autoLogin" field to LoginFinishedEvent

### v1.1.2 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.1...v1.1.2)

* New Features
  * Implemented auto login

* Changes
  * ProviderNotFoundException is now a RuntimeException

### v1.1.1 [view commit logs](https://github.com/soomla/android-profile/compare/v1.1.0...v1.1.1)

* Changes
  * Upgraded simpla-facebook to support newer version of FB SDK.

* Fixes
  * Improved working with permissions in FB - auto requesting permissions

### v1.1.0 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.6...v1.1.0)

* New Features
  * Implemented Pagination for getContacts and getFeed
  * Implemented multi-sharing (sharing with the native functionality of your target platform)
  * Implemented methods to show confirmation dialog before some actions
  * Supporting permissions param in FB

* Changes
  * Check permissions before every action in FB

* Fixes
  * Fixing getting birthday for user profile

### v1.0.6 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.5...v1.0.6)

* Changes
  * Fixed error log
  * Updated core module

### v1.0.5 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.4...v1.0.5)

* Fixes
  * Login cancelled event fires when going back from Twitter login web view

### v1.0.4 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.3...v1.0.4)

* Fixes
  * Supporting foreground/background events on versions >= ICS

* New Features
  * Added support for upload image with File

### v1.0.3 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.2...v1.0.3)

* Fixes
  * Giving rewards before sending events

* New Features
  * Upload bitmap on Google+
  * Supporting new Foreground service from Core

### v1.0.2 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.1...v1.0.2)

* Fixes
  * If login success listener has an exception, we fail gracefully and not letting onException take it. This is how we prevent LoginFailedEvent right after LoginFinishedEvent.

### v1.0.1 [view commit logs](https://github.com/soomla/android-profile/compare/v1.0.0...v1.0.1)
* Changes
  * Updated Core submodule

### v1.0.0 (16.11.14)
* Features
  * The module is integrated with Facebook, Google Plus and Twitter
  * Ability to preform following actions on multiple social networks (parallel):
    * Login/Logout
    * Update status
    * Update Story (supported fully in Facebook only)
    * Upload image
    * Get user profile + store it on the device
    * Get user's contacts (not all social networks provide all information)
    * Get user's most recent feed (not supported in Google Plus)
