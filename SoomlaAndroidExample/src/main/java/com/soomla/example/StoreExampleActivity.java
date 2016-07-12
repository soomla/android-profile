/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.soomla.example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.soomla.BusProvider;
import com.soomla.Soomla;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaConfig;
import com.soomla.SoomlaUtils;
import com.soomla.profile.SoomlaProfile;
import com.soomla.profile.domain.IProvider;
import com.soomla.store.*;
import com.soomla.store.billing.google.GooglePlayIabService;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.events.SoomlaStoreInitializedEvent;
import com.soomla.store.exceptions.VirtualItemNotFoundException;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Map;

/**
 * In this class <code>StoreController</code> and <code>EventHandler</code> are initialized before
 * the store is opened. This class is responsible for displaying the initial screen of the game,
 * which contains a drag and drop image which leads to the next display and activity:
 * <code>StoreGoodsActivity</code>.
 */
public class StoreExampleActivity extends Activity {

    IStoreAssets storeAssets = new MuffinRushAssets();
    ProgressDialog progress;

    @Override
    protected void onResume() {
        super.onResume();
    }


    boolean callLevelUpInit = true;

    /**
     * Called when the activity starts.
     * Displays the main UI screen of the game.
     *
     * @param savedInstanceState if the activity should be re-initialized after previously being
     *                           shut down then this <code>Bundle</code> will contain the most
     *                           recent data, otherwise it will be null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        BusProvider.getInstance().register(this);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

		SoomlaConfig.logDebug = true;

//        PurchasingManager.registerObserver(new PurchasingObserver(this));

        mRobotView = (ImageView) findViewById(R.id.drag_img);
        mRobotView.setOnTouchListener(new MyTouchListener());
        findViewById(R.id.rightbox).setOnDragListener(new MyDragListener());

        Typeface font = Typeface.createFromAsset(getAssets(), "GoodDog.otf");
        ((TextView) findViewById(R.id.title_text)).setTypeface(font);
        ((TextView) findViewById(R.id.main_text)).setTypeface(font);

        /*
         Initialize StoreController and EventHandler before the store is opened.

         Compute your public key (that you got from the Android Market publisher site).

         Instead of just storing the entire literal string here embedded in the program,
         construct the key at runtime from pieces or use bit manipulation (for example,
         XOR with some other string) to hide the actual key. The key itself is not secret
         information, but we don't want to make it easy for an adversary to replace the
         public key with one of their own and then fake messages from the server.

         Generally, encryption keys/passwords should only be kept in memory
         long enough to perform the operation they need to perform.
        */

        mEventHandler = new ExampleEventHandler(mHandler, this);

        Soomla.initialize("[CUSTOM SECRET HERE]");

        SoomlaStore.getInstance().initialize(storeAssets);
        GooglePlayIabService.getInstance().setPublicKey("[YOUR PUBLIC KEY FROM THE MARKET]");
        GooglePlayIabService.AllowAndroidTestPurchases = true;

        Map<IProvider.Provider, Map<String, String>> providerParams = new HashMap<>();

        Map<String, String> facebookParams = new HashMap<>();
        facebookParams.put("permissions", "email,user_friends");
        facebookParams.put("autoLogin", Boolean.toString(true));

        Map<String, String> twitterParams = new HashMap<String, String>();
        twitterParams.put("consumerKey", "[YOUR CONSUMER KEY]");
        twitterParams.put("consumerSecret", "[YOUR CONSUMER SECRET]");
        twitterParams.put("autoLogin", Boolean.toString(true));

        Map<String, String> googleParams = new HashMap<String, String>();
        googleParams.put("autoLogin", Boolean.toString(true));

        providerParams.put(IProvider.Provider.FACEBOOK, facebookParams);
        providerParams.put(IProvider.Provider.TWITTER, twitterParams);
        providerParams.put(IProvider.Provider.GOOGLE, googleParams);


        SoomlaProfile.getInstance().initialize(this, providerParams);
    }

    @Subscribe
    public void onSoomlaStoreInitializedEvent(SoomlaStoreInitializedEvent soomlaStoreInitializedEvent) {
        //FOR TESTING PURPOSES ONLY: Check if it's a first run, if so add 10000 currencies.
        SharedPreferences prefs =
                SoomlaApp.getAppContext().getSharedPreferences("config",
                        Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(FIRST_RUN, false);
        if (!initialized) {
            try {
                for (VirtualCurrency currency : storeAssets.getCurrencies()) {
                    StoreInventory.giveVirtualItem(currency.getItemId(), 10000);
                }
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(FIRST_RUN, true);
                edit.commit();
            } catch (VirtualItemNotFoundException e) {
                SoomlaUtils.LogError("Example Activity", "Couldn't add first 10000 currencies.");
            }
        }

        progress.dismiss();
    }

    /**
     * Puts the SOOMBOT back on the left side of the screen after it was dragged by the user to
     * the right (the empty box).
     */
    public void robotBackHome(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup left = (ViewGroup)findViewById(R.id.leftbox);
                ViewGroup right = (ViewGroup)findViewById(R.id.rightbox);

                if (mRobotView.getParent() != left){
                    right.removeView(mRobotView);
                    left.addView(mRobotView);
                }
            }
        });
    }

    private final class MyTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }

    private final class MyDragListener implements View.OnDragListener {
        Drawable enterShape = getResources().getDrawable(R.drawable.shape_droptarget);
        Drawable normalShape = getResources().getDrawable(R.drawable.shape);

        @Override
        public boolean onDrag(View v, DragEvent event) {
            View view = (View) event.getLocalState();
//            ViewGroup owner = (ViewGroup) view.getParent();
//            LinearLayout container = (LinearLayout) v;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundDrawable(enterShape);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundDrawable(normalShape);
                    break;
                case DragEvent.ACTION_DROP:

                    // Dropped, reassign View to ViewGroup

                    ViewGroup left = (ViewGroup)findViewById(R.id.leftbox);
                    ViewGroup right = (ViewGroup)findViewById(R.id.rightbox);

                    if (right == v){
                        left.removeView(view);
                        right.addView(view);
                        view.setVisibility(View.VISIBLE);

                        // Once the user drags the SOOMBOT to the empty box, we open the store.
                        openStore();
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    view.setVisibility(View.VISIBLE);

                    v.setBackgroundDrawable(normalShape);
                default:
                    break;
            }
            return true;
        }
    }

    private void openStore() {
        Intent intent = new Intent(getApplicationContext(), StoreGoodsActivity.class);
        startActivity(intent);
        robotBackHome();
    }


    /** Private Members */

    private Handler mHandler = new Handler();

    private ImageView mRobotView;

    private ExampleEventHandler mEventHandler;

    private static final String PREFS_NAME = "store.prefs";

    private static final String FIRST_RUN = "a#AA#BB#C";
}

