package com.example.encryptor;

import static com.example.encryptor.FileUtils.getFileFromUri;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

public class PhotoActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private static final int normalSize = 14;
    private TabLayoutMediator mediator;
    private String filePath = "";

    public interface onDataChangeListener {
        void onDataChange(String data);
    }

    private onDataChangeListener mDataChangeListener;

    public void setOnDataChange(onDataChangeListener dataChangeListener) {
        mDataChangeListener = dataChangeListener;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.song_detail_toolbar_menu_share) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.titel)//标题
                    .setMessage(R.string.info)//内容
                    .setIcon(R.mipmap.lock)//图标
                    .setCancelable(true)
                    .show();
        } else if (item.getItemId() == android.R.id.home) {
            Intent intent1 = new Intent(this, TextActivity.class);
            startActivity(intent1);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveActionSend(intent);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.no_slide, R.anim.out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
        setContentView(R.layout.activity_photo);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.view_pager);

        final String[] tabs = new String[]{getString(R.string.encode), getString(R.string.decode)};
        //禁用预加载
        //viewPager2.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
        //Adapter
        viewPager2.setAdapter(new FragmentStateAdapter(getSupportFragmentManager(), getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                //FragmentStateAdapter内部自己会管理已实例化的fragment对象。
                // 所以不需要考虑复用的问题
                if (position == 0) {
                    return FragmentEn.newInstance(null);
                } else {
                    if (filePath.isEmpty()){
                        return FragmentDe.newInstance(null);
                    }else{
                        return FragmentDe.newInstance(filePath);
                    }
                }
            }

            @Override
            public int getItemCount() {
                return tabs.length;
            }
        });
        //viewPager 页面切换监听
        viewPager2.registerOnPageChangeCallback(changeCallback);
        mediator = new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            //这里可以自定义TabView
            TextView tabView = new TextView(PhotoActivity.this);
            tabView.setGravity(Gravity.CENTER);
            tabView.setText(tabs[position]);
            tabView.setTextSize(normalSize);
            tab.setCustomView(tabView);
        });
        //要执行这一句才是真正将两者绑定起来
        mediator.attach();

        verifyStoragePermissions(this);

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String idm = sharedPreferences.getString("id", "");
        if (idm.equals("")) {
           new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.titel)//标题
                    .setMessage(R.string.info)//内容
                    .setIcon(R.mipmap.lock)//图标
                    .setCancelable(false)
                    .setPositiveButton(R.string.back, (dialog, which) -> {
                        Intent intent1 = new Intent(this, TextActivity.class);
                        startActivity(intent1);
                        finish();
                    })
                    .show();
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                receiveActionSend(intent);
            }
        }
    }

    @SuppressLint("ResourceType")
    public void receiveActionSend(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        //判断action事件
        if (type == null || !Intent.ACTION_VIEW.equals(action)) {
            return;
        }
        //取出文件uri
        Uri uri = intent.getData();
        if (uri == null) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        //获取文件真实地址
        filePath = getFileFromUri(getApplicationContext(), uri);
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        viewPager2.setCurrentItem(1);
        if (mDataChangeListener != null) {
            mDataChangeListener.onDataChange(filePath);
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_Media = 2;
    private static final String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final String[] PERMISSIONS_Media = {"android.permission.READ_MEDIA_IMAGES"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission == PackageManager.PERMISSION_DENIED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
            int permission1 = ActivityCompat.checkSelfPermission(activity, "android.permission.READ_MEDIA_IMAGES");
            if (permission1 == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_Media, REQUEST_Media);
            }
        } catch (Exception e) {
            Toast.makeText(activity.getApplicationContext(), "???", Toast.LENGTH_SHORT).show();
        }
    }

    private final ViewPager2.OnPageChangeCallback changeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            //可以来设置选中时tab的大小
            int tabCount = tabLayout.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                TextView tabView;
                if (tab != null) {
                    tabView = (TextView) tab.getCustomView();
                    if (tab.getPosition() == position) {
                        if (tabView != null) {
                            int activeSize = 20;
                            tabView.setTextSize(activeSize);
                            tabView.setTypeface(Typeface.DEFAULT_BOLD);
                        }
                    } else {
                        assert tabView != null;
                        tabView.setTextSize(normalSize);
                        tabView.setTypeface(Typeface.DEFAULT);
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        mediator.detach();
        viewPager2.unregisterOnPageChangeCallback(changeCallback);
        super.onDestroy();
    }
}