package ir.hashemi.market;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import ir.hashemi.market.adapter.AdapterFullScreenImage;
import ir.hashemi.market.utils.Tools;

public class ActivityFullScreenImage extends AppCompatActivity {

    public static final String EXTRA_POS = "key.EXTRA_POS";
    public static final String EXTRA_IMGS = "key.EXTRA_IMGS";

    private AdapterFullScreenImage adapter;
    private ViewPager viewPager;
    private TextView text_page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_full_screen_image);

        applyCloseButtonInsets();

        viewPager = (ViewPager) findViewById(R.id.pager);
        text_page = (TextView) findViewById(R.id.text_page);

        ArrayList<String> items = new ArrayList<>();
        Intent i = getIntent();
        final int position = i.getIntExtra(EXTRA_POS, 0);
        items = i.getStringArrayListExtra(EXTRA_IMGS);
        adapter = new AdapterFullScreenImage(ir.hashemi.market.ActivityFullScreenImage.this, items);
        final int total = adapter.getCount();
        viewPager.setAdapter(adapter);

        text_page.setText(String.format(getString(R.string.image_of), (position + 1), total));

        // displaying selected image first
        viewPager.setCurrentItem(position);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int pos, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int pos) {
                text_page.setText(String.format(getString(R.string.image_of), (pos + 1), total));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        ((ImageButton) findViewById(R.id.btnClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // for system bar in lollipop
        Tools.systemBarLolipop(this);
    }

    private void applyCloseButtonInsets() {
        View root = findViewById(R.id.full_screen_root);
        View closeButton = findViewById(R.id.btnClose);
        View pageIndicator = findViewById(R.id.text_page);
        RelativeLayout.LayoutParams initialParams =
                (RelativeLayout.LayoutParams) closeButton.getLayoutParams();
        final int initialTopMargin = initialParams.topMargin;
        final int initialRightMargin = initialParams.rightMargin;
        RelativeLayout.LayoutParams initialIndicatorParams =
                (RelativeLayout.LayoutParams) pageIndicator.getLayoutParams();
        final int initialBottomMargin = initialIndicatorParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeArea = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            ViewGroup.LayoutParams rawParams = closeButton.getLayoutParams();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) rawParams;
            params.topMargin = initialTopMargin + safeArea.top;
            params.rightMargin = initialRightMargin + safeArea.right;
            closeButton.setLayoutParams(params);

            Insets safeBottom = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            RelativeLayout.LayoutParams indicatorParams =
                    (RelativeLayout.LayoutParams) pageIndicator.getLayoutParams();
            indicatorParams.bottomMargin = initialBottomMargin + safeBottom.bottom;
            pageIndicator.setLayoutParams(indicatorParams);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }


}

