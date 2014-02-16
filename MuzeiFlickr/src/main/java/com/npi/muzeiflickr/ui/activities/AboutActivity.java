package com.npi.muzeiflickr.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.npi.muzeiflickr.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Created by nicolas on 14/02/14.
 * Activity displaying informations about the developer
 */
public class AboutActivity extends Activity {


    private LinearLayout card2;
    private LinearLayout card1;
    private int number = 0;

    public static void launchActivity(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        CalligraphyConfig.initDefault("");

        card1 = (LinearLayout) findViewById(R.id.card1);
        card2 = (LinearLayout) findViewById(R.id.card2);


        TextView developedBy = (TextView) findViewById(R.id.developed_by);
        developedBy.setText(Html.fromHtml(getString(R.string.developed_by)));


        ImageView gplus = (ImageView) findViewById(R.id.google_plus);
        ImageView twitter = (ImageView) findViewById(R.id.twitter);
        ImageView blog = (ImageView) findViewById(R.id.blog);

        gplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/113671876130843889747"));
                startActivity(browserIntent);
            }
        });
        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/pomepuyn"));
                startActivity(browserIntent);
            }
        });
        blog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://nicolaspomepuy.fr"));
                startActivity(browserIntent);
            }
        });

        TextView muzei = (TextView) findViewById(R.id.muzei);
        TextView retrofit = (TextView) findViewById(R.id.retrofit);
        TextView calligraphy = (TextView) findViewById(R.id.calligraphy);
        TextView betterpickers = (TextView) findViewById(R.id.betterpickers);

        muzei.setText(Html.fromHtml(getString(R.string.desc_muzei)));
        retrofit.setText(Html.fromHtml(getString(R.string.desc_retrofit)));
        calligraphy.setText(Html.fromHtml(getString(R.string.desc_calligraphy)));
        betterpickers.setText(Html.fromHtml(getString(R.string.desc_betterpickers)));

        Linkify.addLinks(muzei, Linkify.ALL);
        Linkify.addLinks(retrofit, Linkify.ALL);
        Linkify.addLinks(calligraphy, Linkify.ALL);
        Linkify.addLinks(betterpickers, Linkify.ALL);

        card1.postDelayed(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator anim = ObjectAnimator.ofFloat(card1, "translationY", -card1.getHeight(), 0);
                anim.setInterpolator(new OvershootInterpolator());
                anim.setDuration(1000);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        card1.setVisibility(View.VISIBLE);
                        card2.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
                ObjectAnimator anim2 = ObjectAnimator.ofFloat(card2, "translationY", -(card2.getHeight() + card1.getHeight()), 0);
                anim2.setInterpolator(new OvershootInterpolator());
                anim2.setDuration(1000);

                ObjectAnimator anim3 = ObjectAnimator.ofFloat(card1, "alpha", 0, 1);
                anim3.setDuration(1500);
                ObjectAnimator anim4 = ObjectAnimator.ofFloat(card2, "alpha", 0, 1);
                anim4.setDuration(1500);


                AnimatorSet set = new AnimatorSet();
                set.playTogether(anim, anim2, anim3, anim4);
                set.start();
            }
        }, 600);

        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (number > 5) {
                    Toast.makeText(AboutActivity.this, "Yeah!!!", Toast.LENGTH_LONG).show();

                    ObjectAnimator anim = ObjectAnimator.ofFloat(card1, "rotation", 0, 360);
                    ObjectAnimator anim1 = ObjectAnimator.ofFloat(card2, "rotation", 0, 360);
                    ObjectAnimator anim2 = ObjectAnimator.ofFloat(card1, "translationY", 0, card1.getHeight(), 0);
                    ObjectAnimator anim3 = ObjectAnimator.ofFloat(card2, "translationY", 0, -card1.getHeight(), 0);
                    ObjectAnimator anim4 = ObjectAnimator.ofFloat(card1, "scaleX", 1f, 0.5f, 1f);
                    ObjectAnimator anim5 = ObjectAnimator.ofFloat(card2, "scaleX", 1f, 0.5f, 1f);
                    ObjectAnimator anim6 = ObjectAnimator.ofFloat(card1, "scaleY", 1f, 0.5f, 1f);
                    ObjectAnimator anim7 = ObjectAnimator.ofFloat(card2, "scaleY", 1f, 0.5f, 1f);
                    anim.setInterpolator(new AnticipateOvershootInterpolator());
                    anim1.setInterpolator(new AnticipateOvershootInterpolator());
                    anim2.setInterpolator(new AnticipateOvershootInterpolator());
                    anim3.setInterpolator(new AnticipateOvershootInterpolator());
                    anim4.setInterpolator(new AnticipateOvershootInterpolator());
                    anim5.setInterpolator(new AnticipateOvershootInterpolator());
                    anim6.setInterpolator(new AnticipateOvershootInterpolator());
                    anim7.setInterpolator(new AnticipateOvershootInterpolator());
                    int duration = 1500;
                    anim.setDuration(duration);
                    anim1.setDuration(duration);
                    anim2.setDuration(duration);
                    anim3.setDuration(duration);
                    anim4.setDuration(duration);
                    anim5.setDuration(duration);
                    anim6.setDuration(duration);
                    anim7.setDuration(duration);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(anim, anim1, anim2, anim3, anim4, anim5, anim6, anim7);
                    set.start();


                    number = 0;
                } else {
                    number++;
                }
            }
        };
        card1.setOnClickListener(onClickListener);
        card2.setOnClickListener(onClickListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
