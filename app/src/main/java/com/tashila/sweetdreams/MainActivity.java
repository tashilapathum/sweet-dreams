package com.tashila.sweetdreams;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.RandomTransitionGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, PurchasesUpdatedListener, BillingClientStateListener {
    //always set activeLayout before calling showNextScreen

    private BillingClient billingClient;
    BillingFlowParams flowParams;
    private List<BillingFlowParams> flowParamsList;
    public static final String TAG = "MainActivity";
    private LinearLayout homeLayout;
    private LinearLayout timesLayout;
    private LinearLayout aboutLayout;
    private LinearLayout learnLayout;
    private LinearLayout supportLayout;
    private LinearLayout activeLayout; //for home
    private LinearLayout bottomMenu;
    private Button btnWakeUpAt;
    private Button btnIfSleptAt;
    private int timePickerId;
    private LocalDateTime[] times;
    private String topText1;
    private String topText2;
    private String bottomText;
    private int buyButtonId;
    SharedPreferences sharedPref;
    private int sleepingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidThreeTen.init(this);
        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);
        sleepingTime = sharedPref.getInt("sleepingTime", 14);

        //to make fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        //background image animation
        try {
            boolean bgAnimCrashed = sharedPref.getBoolean("bgAnimCrashed", false);
            if (!bgAnimCrashed) {
                KenBurnsView kbvBack = findViewById(R.id.kbvBack);
                kbvBack.setVisibility(View.VISIBLE);
                AccelerateDecelerateInterpolator adi = new AccelerateDecelerateInterpolator();
                RandomTransitionGenerator generator = new RandomTransitionGenerator(42000, adi);
                kbvBack.setTransitionGenerator(generator);
            }
            else {
                findViewById(R.id.parent_layout).setBackground(getResources().getDrawable(R.drawable.background));
            }
        }
        catch (Exception e) {
            FirebaseCrashlytics.getInstance().log(e.getMessage());
            sharedPref.edit().putBoolean("bgAnimCrashed", true).apply();
            findViewById(R.id.parent_layout).setBackground(getResources().getDrawable(R.drawable.background));
        }

        times = new LocalDateTime[7]; //because 7 means 6th imageView

        //layouts
        homeLayout = findViewById(R.id.home_layout);
        timesLayout = findViewById(R.id.times_layout);
        aboutLayout = findViewById(R.id.about_layout);
        learnLayout = findViewById(R.id.learn_layout);
        supportLayout = findViewById(R.id.support_layout);
        bottomMenu = findViewById(R.id.bottom_menu);

        //buttons
        Button btnSleepNow = findViewById(R.id.btnSleepNow);
        btnWakeUpAt = findViewById(R.id.btnWakeUpAt);
        btnIfSleptAt = findViewById(R.id.btnIfSleptAt);
        Button btnTakeNap = findViewById(R.id.btnTakeNap);

        //on click actions
        btnSleepNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                whenSleeping(null);
                activeLayout = timesLayout;
            }
        });
        btnWakeUpAt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTimePicker(view.getId());
            }
        });
        btnIfSleptAt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTimePicker(view.getId());
            }
        });
        btnTakeNap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeNap();
                activeLayout = timesLayout;
            }
        });
        ImageButton imTime = findViewById(R.id.sleepingTime);
        imTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSleepingTime dialogSleepingTime = new DialogSleepingTime();
                dialogSleepingTime.show(getSupportFragmentManager(), "sleeping time dialog");
            }
        });

        //hide bottom menu when landscape
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            bottomMenu.setVisibility(View.GONE);

        //rate dialog
        boolean alreadyRated = sharedPref.getBoolean("alreadyRated", false);
        int openCount = sharedPref.getInt("openCount", 0);
        sharedPref.edit().putInt("openCount", openCount + 1).apply();
        if (!alreadyRated & openCount >= 14) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Enjoying the app?")
                    .setMessage("If you like \"Sweet Dreams\", would you mind rating the app 5 stars on PlayStore? " +
                            "\n\nAlso if you have any problem with the app or want to request a new feature, " +
                            "please let me know in the review so I can help you. I read every review :)")
                    .setPositiveButton("Rate", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sharedPref.edit().putBoolean("alreadyRated", true).apply();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.tashila.sweetdreams"));
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sharedPref.edit().putInt("openCount", 0).apply();
                        }
                    })
                    .setNeutralButton("Never", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sharedPref.edit().putBoolean("alreadyRated", true).apply();
                        }
                    })
                    .show();
        }
    }

    private void whenSleeping(LocalDateTime laterTime) {
        LocalDateTime currentTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        if (laterTime == null) {
            currentTime = LocalDateTime.now();
            topText1 = "If you go to bed right now...";
        } else {
            currentTime = laterTime;
            topText1 = "If you go to bed at " + laterTime.format(formatter) + "...";
        }
        Toast.makeText(this, "Time: "+sleepingTime, Toast.LENGTH_SHORT).show();
        currentTime = currentTime.plusMinutes(sleepingTime); //time to fall asleep
        for (int i = 1; i <= 6; i++) {
            //prepare data
            currentTime = currentTime.plusMinutes(90);
            times[i] = currentTime;
            String wakeUpTime = currentTime.format(formatter);

            //set time
            int timeViewId = getResources().getIdentifier("time" + i, "id", getPackageName());
            TextView tvTime = findViewById(timeViewId);
            tvTime.setVisibility(View.VISIBLE);
            tvTime.setText(wakeUpTime);
            if (i == 3 || i == 4)
                tvTime.append("*");

            //turn on alarm icons
            int alarmViewId = getResources().getIdentifier("alarm" + i, "id", getPackageName());
            findViewById(alarmViewId).setVisibility(View.VISIBLE);
        }

        showTips(findViewById(R.id.alarm1));

        topText2 = "It's best if you try to wake up at one of the following times:";
        bottomText = "*Recommended " +
                "\n\nA proper sleep consists of 5 or 6 full sleep cycles. So the third and fourth wake up times are recommended." +
                "\n\nNote that you should be falling asleep at these times. So plan accordingly.\nAverage person takes 14 minutes to fall asleep.";
        showNextScreen(homeLayout, timesLayout, false);
    }

    private void whenWakingUp(LocalDateTime wakeUpTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        topText1 = "If you need to wake up at " + wakeUpTime.format(formatter) + "...";
        topText2 = "It's best if you try to fall asleep at one of the following times:";
        wakeUpTime = wakeUpTime.minusMinutes(180); //3 (minus 1 because added in the loop) minimum sleep cycles
        for (int i = 1; i <= 6; i++) {
            wakeUpTime = wakeUpTime.minusMinutes(90);
            String sleepTime = wakeUpTime.format(formatter);
            int timeViewId = getResources().getIdentifier("time" + i, "id", getPackageName());
            int alarmViewId = getResources().getIdentifier("alarm" + i, "id", getPackageName());
            findViewById(alarmViewId).setVisibility(View.GONE);
            TextView tvTime = findViewById(timeViewId);
            if (i >= 5) tvTime.setVisibility(View.GONE);
            else {
                tvTime.setVisibility(View.VISIBLE);
                tvTime.setText(sleepTime);
                if (i == 2 || i == 3)
                    tvTime.append("*");
            }
        }

        bottomText = "*Recommended\n\n" +
                "A good sleep consists of 5 or 6 full sleep cycles. " +
                "An average person requires minimum 6 hours or maximum 8 hours of sleep to function properly. " +
                "So the second and third wake up times are recommended.";
        showNextScreen(homeLayout, timesLayout, false);
    }

    private void takeNap() {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        LocalDateTime timeNow = LocalDateTime.now();
        topText1 = "If you take a nap right now...";
        topText2 = "It's best if you try to wake up at one of the following times:";
        timeNow = timeNow.plusMinutes(16); //minimum nap time minus 5
        for (int i = 1; i <= 6; i++) {
            timeNow = timeNow.plusMinutes(5);
            times[i] = timeNow;
            String wakeUpTime = timeNow.format(formatter);
            int timeViewId = getResources().getIdentifier("time" + i, "id", getPackageName());
            int alarmViewId = getResources().getIdentifier("alarm" + i, "id", getPackageName());
            TextView tvTime = findViewById(timeViewId);
            if (i > 3) {
                tvTime.setVisibility(View.GONE);
                findViewById(alarmViewId).setVisibility(View.GONE);
            } else {
                tvTime.setText(wakeUpTime);
                findViewById(alarmViewId).setVisibility(View.VISIBLE);
            }
        }

        showTips(findViewById(R.id.alarm1));

        bottomText = "A nap should not be longer than 30 minutes. So these times are recommended to get a quick energy boost.";
        showNextScreen(homeLayout, timesLayout, false);
    }

    private void showNextScreen(final View currentView, final View replacementView, final boolean showHome) {
        //times
        currentView.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        replacementView.animate()
                                .alpha(1.0f)
                                .setDuration(500)
                                .setStartDelay(500)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        replacementView.setAlpha(1.0f);
                                    }
                                });
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        currentView.setVisibility(View.GONE);
                        currentView.setAlpha(0.0f);
                        replacementView.setVisibility(View.VISIBLE);

                        //bottom menu
                        if (showHome) {
                            bottomMenu.animate()
                                    .alpha(1.0f)
                                    .translationY(-bottomMenu.getHeight())
                                    .setDuration(250)
                                    .setListener(new Animator.AnimatorListener() {
                                        @Override
                                        public void onAnimationStart(Animator animator) {
                                            bottomMenu.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onAnimationEnd(Animator animator) {
                                            bottomMenu.clearAnimation();
                                        }

                                        @Override
                                        public void onAnimationCancel(Animator animator) {

                                        }

                                        @Override
                                        public void onAnimationRepeat(Animator animator) {

                                        }
                                    })
                                    .start();
                        } else {
                            bottomMenu.animate()
                                    .alpha(0.0f)
                                    .translationY(bottomMenu.getHeight())
                                    .setDuration(250)
                                    .setListener(new Animator.AnimatorListener() {
                                        @Override
                                        public void onAnimationStart(Animator animator) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animator animator) {
                                            bottomMenu.clearAnimation();
                                            bottomMenu.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onAnimationCancel(Animator animator) {

                                        }

                                        @Override
                                        public void onAnimationRepeat(Animator animator) {

                                        }
                                    })
                                    .start();
                        }
                    }
                });

        //hide bottom menu when landscape
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            bottomMenu.setVisibility(View.GONE);

        //top texts
        TextView tvTopText1 = findViewById(R.id.tvTopText1);
        TextView tvTopText2 = findViewById(R.id.tvTopText2);
        TextView tvBottomText = findViewById(R.id.tvBottomText);
        tvTopText1.setText(topText1);
        tvTopText2.setText(topText2);
        tvBottomText.setText(bottomText);
    }

    @Override
    public void onBackPressed() {
        if (homeLayout.getVisibility() == View.GONE)
            showNextScreen(activeLayout, homeLayout, true);
        else
            super.onBackPressed();
    }

    private void openTimePicker(int viewId) {
        timePickerId = viewId;
        DialogFragment timePicker = new TimePickerFragment();
        timePicker.show(getSupportFragmentManager(), "time picker");
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        LocalDateTime time = LocalDateTime.of(LocalDate.now().getYear(), LocalDate.now().getMonth(),
                LocalDate.now().getDayOfMonth(), hour, minute);
        if (timePickerId == btnWakeUpAt.getId()) whenWakingUp(time);
        if (timePickerId == btnIfSleptAt.getId()) whenSleeping(time);
        activeLayout = timesLayout;
    }


    public void showAlarm(View view) {
        Intent alarmIntent = null;
        for (int i = 1; i < 7; i++) {
            String alarmTag = view.getResources().getResourceName(view.getId());
            alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            if (alarmTag.contains(String.valueOf(i))) {
                alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Wake up!");
                alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, times[i].getHour());
                alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, times[i].getMinute());
                break;
            }
        }
        startActivity(alarmIntent);
    }

    public void onClickAbout(View view) {
        showNextScreen(homeLayout, aboutLayout, false);
        activeLayout = aboutLayout;
    }

    public void onClickLearnMore(View view) {

        String[] questions = {
                "How are these times calculated?",
                "What is a sleep cycle?",
                "What if I take more or less than 14 minutes to fall asleep?",
                "Why there are no ads? Is there a pro version?"
        };
        String[] answers = {
                "They are calculated using sleep cycles. You feel tired when you wake up in the middle of a sleep cycle. This app calculates the time to wake up or sleep so you'll wake up between sleep cycles, when you're in the light sleep. So you'll feel refreshed and well-rested after waking up.",
                "Sleep cycles are part of our internal biological “clocks” the regularly occurring patterns of brain waves which occur while we sleep. Sleep cycles typically last around ninety minutes to two hours, during which time the brain cycles from slow-wave sleep to REM sleep in which we experience dreams.",
                "For now, you'll have to adjust the alarm time before setting the alarm. An option to change the time to fall asleep will be added in the future.",
                "When you're trying to sleep, the last thing you need is an video ad in the face. And there's no pro version or paid items. \"Sweet Dreams\" will forever be free, including all upcoming features. This app solely depends on your donations (:"
        };

        learnLayout.removeAllViews();
        for (int i = 0; i < questions.length; i++) {
            View sampleFAQ = getLayoutInflater().inflate(R.layout.sample_faq, null);
            TextView question = sampleFAQ.findViewById(R.id.question);
            TextView answer = sampleFAQ.findViewById(R.id.answer);
            question.setText(questions[i]);
            answer.setText(answers[i]);
            learnLayout.addView(sampleFAQ);
        }

        View btnHome = getLayoutInflater().inflate(R.layout.home_button, null);
        learnLayout.addView(btnHome);


        activeLayout = learnLayout;
        showNextScreen(homeLayout, learnLayout, false);
    }

    private void showTips(View view) {
        boolean alreadyShown = sharedPref.getBoolean("alarmTipShown", false);
        if (!alreadyShown) {
            new BubbleShowCaseBuilder(this)
                    .targetView(view)
                    .title("Set alarms")
                    .description("Tap any alarm icon to set the alarm for the relevant time")
                    .show();
            sharedPref.edit().putBoolean("alarmTipShown", true).apply();
        }
    }

    public void onClickRate(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.tashila.sweetdreams"));
        startActivity(intent);
    }

    public void onClickMoreApps(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/developer?id=Tashila+Pathum"));
        startActivity(intent);
    }

    public void onClickHome(View view) {
        showNextScreen(activeLayout, homeLayout, true);
    }

    public void onClickDonate(View view) {
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(this);
        activeLayout = supportLayout;
        showNextScreen(homeLayout, supportLayout, false);
    }

    //------------------------------------------IAP-----------------------------------------------//

    public void onClickBuy(View view) {
        buyButtonId = view.getId();
        continueBuy();
    }

    private void continueBuy() {
        if (isNetworkAvailable()) {
            for (int i=0; i<flowParamsList.size(); i++) {
                switch (buyButtonId) {
                    case R.id.b1: {
                        flowParams = flowParamsList.get(0);
                        break;
                    }
                    case R.id.b2: {
                        flowParams = flowParamsList.get(1);
                        break;
                    }
                    case R.id.b3: {
                        flowParams = flowParamsList.get(2);
                        break;
                    }
                }
            }
            billingClient.launchBillingFlow(this, flowParams);
        }
        else {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.connec_failed)
                    .setMessage(R.string.must_have_internet)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            continueBuy();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null)
            for (Purchase purchase : list)
                handlePurchase(purchase);
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED)
            Toast.makeText(this, R.string.p_cancelled, Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, R.string.p_failed, Toast.LENGTH_SHORT).show();


    }

    private void handlePurchase(Purchase purchase) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.ty)
                .setMessage(R.string.ty_des)
                .setPositiveButton(android.R.string.ok, null)
                .show();

        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String s) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                   /* new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.ty)
                            .setMessage(R.string.ty_des)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();*/
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "Billing Response OK!");
            querySkuDetails();
            queryPurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.connection_problem)
                .setMessage(R.string.failed_gplay)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recreate();
                    }
                })
                .show();
    }

    public void querySkuDetails() {
        List<String> skuList = new ArrayList<>();
        skuList.add("small_thank_you");
        skuList.add("big_thank_you");
        skuList.add("love_this_app");

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(skuList)
                .build();

        Log.i(TAG, "querySkuDetailsAsync");
        billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                flowParamsList = new ArrayList<>();

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                    for (SkuDetails skuDetails : list) {
                        String sku = skuDetails.getSku();
                        String price = skuDetails.getPrice();

                        //populate list
                        flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .build();
                        for (int i = 0; i < 3; i++)
                            flowParamsList.add(flowParams);

                        //show
                        if (sku.equals("small_thank_you")) {
                            Button btn = findViewById(R.id.b1);
                            btn.setText(price);
                            flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails)
                                    .build();
                            flowParamsList.add(0, flowParams);
                            continue;
                        }
                        if (sku.equals("big_thank_you")) {
                            Button btn = findViewById(R.id.b2);
                            btn.setText(price);
                            flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails)
                                    .build();
                            flowParamsList.add(1, flowParams);
                            continue;
                        }
                        if (sku.equals("love_this_app")) {
                            Button btn = findViewById(R.id.b3);
                            btn.setText(price);
                            flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails)
                                    .build();
                            flowParamsList.add(2, flowParams);
                            continue;
                        }
                    }
                }
            }
        });
    }

    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        billingClient.queryPurchases(BillingClient.SkuType.INAPP);
    }
}


//TODO: use sharedPref as little as possible










