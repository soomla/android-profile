android-profile
===============

*This project is a part of The [SOOMLA](http://www.soom.la) Framework, which is a series of open source initiatives with a joint goal to help mobile game developers do more together. SOOMLA encourages better game design, economy modeling, social engagement, and faster development.*

The gist:

```Java
    //The example below uses Facebook provider, to use different provider change IProvider.Provider.FACEBOOK
    //to IProvider.Provider.Twitter or IProvider.Provider.GooglePlus

    // decide on rewards (here 1 "sword" virtual item) for social action
    Reward reward = new VirtualItemReward([id], "Update Status for item", 1, "sword");

    // optional reward on each action, select from available social providers
    SoomlaProfile.getInstance().login([activity], IProvider.Provider.FACEBOOK, [reward]);


    // after login, the user profile on the selected providers is available locally
    UserProfile userProfile = SoomlaProfile.getInstance().getStoredUserProfile();

    // on successful login event, you can perform actions on the provider
    // this will post to Facebook, upon completion, the user will get 1 sword!
    SoomlaProfile.getInstance().updateStatus(IProvider.Provider.FACEBOOK, "cool game!", reward);
```

## android-profile

**November 16th**: v1.0 **android-profile** supports Facebook, Google+ and Twitter

**android-profile** is an open code initiative as part of The SOOMLA Project. It is a Java API that unifies interaction with social and identity provider APIs, and optionally ties it together with the game's virtual economy.
This enables to easily reward players with social actions they perform in-game, and to leverage user profiles.

![SOOMLA's Profile Module](http://know.soom.la/img/tutorial_img/soomla_diagrams/Profile.png)


## Download

####Pre baked jars in one zip file:

[android-profile v1.2.3](http://library.soom.la/fetch/android-profile/1.2.3?cf=github)

#### From sources:
 - Clone this repository recursively: `git clone --recursive https://github.com/soomla/android-profile.git`
 - Run `./build_all` from project directory
 - Take created binaries from `build` directory and use it!

## Getting Started (With sources)

1. From the downloaded zip, Add the following jars to your project.
    1. `SoomlaAndroidCore.jar`
    1. `AndroidProfile.jar`
    1. `square-otto-1.3.2.jar`
1. Make the following changes to your AndroidManifest.xml:

  Set `SoomlaApp` as the main Application by placing it in the `application` tag:

    ```xml
    <application ...
                 android:name="com.soomla.SoomlaApp">
    ```

1. Initialize `Soomla` with a secret that you chose to encrypt the user data. (For those who came from older versions, this should be the same as the old "custom secret"):

    ```Java
     Soomla.initialize("[YOUR CUSTOM GAME SECRET HERE]");
    ```
    > The secret is your encryption secret for data saved in the DB.

1. Initialize `SoomlaProfile`

  ```Java
    SoomlaProfile.getInstance().initialize();
  ```
  Note that some social providers need special parameters in intialization, you can supply them like so:
  ```Java
    HashMap<IProvider.Provider, HashMap<String, String>> providerParams = new
                HashMap<IProvider.Provider, HashMap<String, String>>();

    // Fill in the HashMap according to social providers

    SoomlaProfile.getInstance().initialize(providerParams);
  ```

  1. **Facebook** - You can provide your custom permission set here.

  	``` java
  	HashMap<String, String> facebookParams = new HashMap<String, String>();
  	facebookParams.put("permissions", "public_profile,user_friends");
  	providerParams.put(IProvider.Provider.FACEBOOK, facebookParams);

  	SoomlaProfile.getInstance().initialize(providerParams);
  	```

    > **NOTE:** You should not request all the possible permissions you'll ever need in your app,
    just request the reasonable minimum. Other permissions will be requested, when they will be needed.
    For instance, if you try to call `updateStatus`, SoomlaProfile will ask for `publish_actions` permission,
    if your app has not got it.

  1. **Google+** - No special parameters needed
  1. **Twitter** - Please provide **Consumer Key** and **Consumer Secret** from the "Keys and Access Tokens" section in [Twitter Apps](https://apps.twitter.com/), like so:
    ```Java
      HashMap<String, String> twitterParams = new HashMap<String, String>();
      twitterParams.put("consumerKey", "[YOUR CONSUMER KEY]");
      twitterParams.put("consumerSecret", "[YOUR CONSUMER SECRET]");

      providerParams.put(IProvider.Provider.TWITTER, twitterParams);
    ```

1. If integrating a virtual economy with the store module, please see [android-store](https://github.com/soomla/android-store) for store setup.

1. Refer to the [next section](https://github.com/soomla/android-profile#whats-next-selecting-social-providers) for information of selecting social providers and setting them up.

Cool, almost there, on to provider selection!


## What's next? Selecting Social Providers

**android-profile** is structured to support multiple social networks (Facebook, Twitter, etc.), at the time of writing this the framework only supports Facebook integration.
We use the [Simple Facebook project](https://github.com/sromku/android-simple-facebook) to support this integration.

### Facebook

Facebook is supported out-of-the-box, you just have to follow the next steps to make it work:

1. From the downloaded zip, Add the following jars to your project.
  1. `AndroidProfileFacebook.jar`
  1. `simple-fb-4.0.3.jar`
  1. `gson-1.7.2.jar`

1. Import the Facebook SDK for Android into your project and setup all the relevant information (Application ID, etc).

    1. For more information regarding this refer to [Facebook SDK for Android](https://developers.facebook.com/docs/android)

    1. SOOMLA uses [Android Studio](https://developer.android.com/sdk/installing/studio.html), in this case you can extract the Facebook SDK into your project folder and then it's simply a case of importing the `iml` file in the Facebook SDK folder into your project
1. Make the following changes in `AndroidManifest.xml`:

      ```xml
      ...

      <application ...
          <activity android:name="com.soomla.profile.social.facebook.SoomlaFacebook$SoomlaFBActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen">
        </activity>
      </application>
      ```

### Twitter

Twitter is also supported out-of-the-box, authentication is done via web view. Follow the next steps to make it work:

> **android-profile** uses the [Twitter4J](https://github.com/yusuke/twitter4j) library (v 4.0.2) to support Twitter integration

1. From the downloaded zip, Add the following jars to your project.
  1. `AndroidProfileTwitter.jar`
  1. `twitter4j-core-4.0.2.jar`
  1. `twitter4j-asyc-4.0.2.jar`

1. Create your Twitter app at https://apps.twitter.com/

1. Make the following changes in `AndroidManifest.xml`:

      ```xml
      ...

      <application ...
          <activity android:name="com.soomla.profile.social.twitter.SoomlaTwitter$SoomlaTwitterActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen">
        </activity>
      </application>
      ```

### Google Plus

1. From the downloaded zip, Add the `AndroidProfileGoogle.jar` jar to your project.

1. Follow [Step 1: Enable the Google+ API](https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api) and create a google+ app for Android.

    > **Note:** Set the PACKAGE NAME of your google+ app to the value the package defined in your `AndroidManifest.xml`.

1. Import `google-play-services_lib` project as module dependency to your project.

    > **Note:** You can either download/copy the existing `google-play-services_lib` project located under [google social provider libs](https://github.com/soomla/android-profile/tree/master/social-providers/android-profile-google/libs) folder or [create one yourself](https://developers.google.com/+/mobile/android/getting-started#step_2_configure_your_eclipse_project).

1. Add `SoomlaGooglePlusActivity` to `AndroidManifest.xml` as following:

      ```xml
      ...

      <application ...
          <activity android:name="com.soomla.profile.social.google.SoomlaGooglePlus$SoomlaGooglePlusActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen">
        </activity>
      </application>
      ```

1. Add the following permissions in `AndroidManifest.xml`:
    ```xml
      <uses-permission android:name="android.permission.INTERNET" />
      <uses-permission android:name="android.permission.GET_ACCOUNTS" />
      <uses-permission android:name="android.permission.USE_CREDENTIALS" />
    ```

## UserProfile

As part of a login call to a provider, Soomla will internally try to also fetch the online user profile details via
`UserProfile` and store them in the secure [Soomla Storage](https://github.com/soomla/android-store#storage--meta-data)
Later, this can be retrieved locally (in offline mode) via:

```java
UserProfile userProfile = SoomlaProfile.getInstance().getStoredUserProfile(IProvider.Provider.FACEBOOK) //depending on your provider
```

This can throw a `UserProfileNotFoundException` if something strange happens to the local storage, in that case, you need to require a new login to get the `UserProfile` again.

## Rewards feature

One of the big benefits of using Soomla's profile module for social networks interactions is that you can easily tie it in with the game's virtual economy.
This is done by the ability to specify a `Reward` (perhaps more specifically, a `VirtualItemReward`) to most social actions defined in `SoomlaProfile`.

For example, to reward a user with a "sword" virtual item upon login to Facebook:
```Java
Reward reward = new VirtualItemReward([id], "Update Status for sword", 1, "sword");
SoomlaProfile.getInstance().login([activity], IProvider.Provider.FACEBOOK, reward);
```

Once login completes sucessfully (wait for `LoginFinishedEvent`, see below on events), the reward will be automatically given, and synchronized with Soomla's storage.

The reward ID is something you manage and should be unique, much like virtual items.


## Debugging

In order to debug android-profile, set `SoomlaConfig.logDebug` to `true`. This will print all of _android-profile's_ debugging messages to logcat.

## Storage

The on-device storage is encrypted and kept in a SQLite database. SOOMLA is preparing a cloud-based storage service that will allow this SQLite to be synced to a cloud-based repository that you'll define.

## Security


If you want to protect your game from 'bad people' (and who doesn't?!), you might want to follow some guidelines:

+ SOOMLA keeps the game's data in an encrypted database. In order to encrypt your data, SOOMLA generates a private key out of several parts of information. The Custom Secret is one of them. SOOMLA recommends that you provide this value when initializing `Soomla` and before you release your game. BE CAREFUL: You can change this value once! If you try to change it again, old data from the database will become unavailable.
+ Following Google's recommendation, SOOMLA also recommends that you split your public key and construct it on runtime or even use bit manipulation on it in order to hide it. The key itself is not secret information but if someone replaces it, your application might get fake messages that might harm it.

## Event Handling


For event handling, we use Square's great open-source project [otto](http://square.github.com/otto/). In ordered to be notified of profile related events, you can register for specific events and create your game-specific behavior to handle them.

> Your behavior is an addition to the default behavior implemented by SOOMLA. You don't replace SOOMLA's behavior.

In order to register for events:

1. In the class that should receive the event create a function with the annotation '@Subscribe'. Example:

    ```Java
    @Subscribe
    public void onLoginFinishedEvent(LoginFinishedEvent loginFinishedEvent) {
        ...
    }
    ```

2. You'll also have to register your class in the event bus (and unregister when needed):

   ```Java
   BusProvider.getInstance().register(this);
   ```

   ```Java
   BusProvider.getInstance().unregister(this);
   ```

> If your class is an Activity, register in 'onResume' and unregister in 'onPause'.

You can find a full event handler example [here](https://github.com/soomla/android-profile/blob/master/SoomlaAndroidExample/src/com/soomla/example/ExampleEventHandler.java).

[List of events](https://github.com/soomla/android-profile/tree/master/SoomlaAndroidProfile/src/com/soomla/profile/events)

[Full documentation and explanation of otto](http://square.github.com/otto/)

## Example Project

The **android-profile** project contains an [example project](https://github.com/soomla/android-profile/tree/master/SoomlaAndroidExample) which shows most of the functionality Profile provides, and the correct setup.
In order to run the project follow this steps:

1. Open the `SoomlaAndroidExample` folder in Android Studio (IntelliJ), it contains an IntelliJ project
1. Setup SDK and out folder, if necessary
1. Run the project

## Facebook Caveats

1. **Facebook Application** - You must create a Facebook application and use its details in your Profile-based application (with Facebook)

1. **Facebook ID** - The Facebook application's ID must be used in your application, this information should be added to the application's `strings.xml` file, under `fb_app_id` (App ID). In the `AndroidManifest.xml` file add the following:
    ```xml
        <application ...
            <activity android:name="com.facebook.LoginActivity" />
            <meta-data android:name="com.facebook.sdk.ApplicationId" android:value="@string/fb_app_id" />
        </application>
    ```
1. **Facebook Permissions** - Profile will request `publish_actions` from the user of the application, to test the application please make sure you test with either Admin, Developer or Tester roles

## Twitter Caveats

1. **Login method returns 401 error** - this could be the result of a few issues:
  1. Have you supplied the correct consumer key and secret SoomlaProfile initialization?
  1. Have you supplied a `Callback URL` in your Twitter application settings?

## Google Plus Caveats

1. Did you set the PACKAGE NAME of your google+ app is the same as the package name in `AndroidManifest.xml`?
1. Did you set the CERTIFICATE FINGERPRINT (SHA1) of your google+ app is the same as your debug.keystore or release keystore SHA1?
1. Did you add google-play-services_lib as a dependency to your project?

Contribution
---
SOOMLA appreciates code contributions! You are more than welcome to extend the capabilities of SOOMLA.

Fork -> Clone -> Implement -> Add documentation -> Test -> Pull-Request.

IMPORTANT: If you would like to contribute, please follow our [Documentation Guidelines](https://github.com/soomla/android-store/blob/master/documentation.md). Clear, consistent comments will make our code easy to understand.

## SOOMLA, Elsewhere ...

+ [Framework Website](http://www.soom.la/)
+ [Knowledge Base](http://know.soom.la/)


<a href="https://www.facebook.com/pages/The-SOOMLA-Project/389643294427376"><img src="http://know.soom.la/img/tutorial_img/social/Facebook.png"></a><a href="https://twitter.com/Soomla"><img src="http://know.soom.la/img/tutorial_img/social/Twitter.png"></a><a href="https://plus.google.com/+SoomLa/posts"><img src="http://know.soom.la/img/tutorial_img/social/GoogleP.png"></a><a href ="https://www.youtube.com/channel/UCR1-D9GdSRRLD0fiEDkpeyg"><img src="http://know.soom.la/img/tutorial_img/social/Youtube.png"></a>

## License

Apache License. Copyright (c) 2012-2014 SOOMLA. http://www.soom.la
+ http://opensource.org/licenses/Apache-2.0
