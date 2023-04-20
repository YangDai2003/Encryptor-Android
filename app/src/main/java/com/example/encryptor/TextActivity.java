package com.example.encryptor;

import static com.example.encryptor.CryptologyUtils.decryptCBC;
import static com.example.encryptor.CryptologyUtils.decryptRSA;
import static com.example.encryptor.CryptologyUtils.encryptCBC;
import static com.example.encryptor.CryptologyUtils.encryptRSA;
import static com.example.encryptor.CryptologyUtils.getPrivateKeyStr;
import static com.example.encryptor.CryptologyUtils.getPublicKeyStr;
import static com.example.encryptor.CryptologyUtils.initKey;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.color.DynamicColors;

import java.security.SecureRandom;
import java.util.Map;

public class TextActivity extends AppCompatActivity {
    private int encode = 0;
    private String publicKey, privateKey;
    TextView textIn, textOut, id;
    Button buttonCopy, buttonShare, buttonChange;
    ImageButton delete, clear, info;
    Button initKey, toPhoto;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("publicKey", publicKey);
        outState.putString("privateKey", privateKey);
        outState.putString("textIn", textIn.getText().toString());
        outState.putString("textOut", textOut.getText().toString());
        outState.putInt("encode", encode);
        outState.putString("id", id.getText().toString());
        outState.putString("mode", buttonChange.getText().toString());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleProcessText(intent);
    }

    protected void initView() {
        toPhoto = findViewById(R.id.buttonPhoto);
        textIn = findViewById(R.id.textViewInput);
        textOut = findViewById(R.id.textViewOutput);
        id = findViewById(R.id.textViewId);
        buttonChange = findViewById(R.id.buttonMode);
        buttonCopy = findViewById(R.id.buttonCopy);
        buttonShare = findViewById(R.id.buttonShare);
        delete = findViewById(R.id.buttonDelete);
        initKey = findViewById(R.id.buttonRsa);
        info = findViewById(R.id.buttonInfo);
        clear = findViewById(R.id.imageButtonClear);
    }

    protected void initViewEvents() {
        id.setOnClickListener(o -> {
            if (id.getText().length() == 0) {
                String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                SecureRandom random = new SecureRandom();
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < 32; ++i) {
                    int number = random.nextInt(str.length());
                    sb.append(str.charAt(number));
                }
                id.setText(sb);
                SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
                //获取Editor对象的引用
                SharedPreferences.Editor editor = sharedPreferences.edit();
                //将获取过来的值放入文件
                editor.putString("id", sb.toString());
                // 提交数据
                editor.apply();
            }
        });
        clear.setOnClickListener(o -> {
            id.setText("");
            //获取SharedPreferences对象
            SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
            //获取Editor对象的引用
            SharedPreferences.Editor editor = sharedPreferences.edit();
            //将获取过来的值放入文件
            editor.remove("id");
            // 提交数据
            editor.apply();
        });
        id.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (encode == 2 || encode == 3) {
                    try {
                        CharSequence res = code(editable);
                        if (!res.equals("")) textOut.setText(res);
                    } catch (Exception ignored) {
                    }
                }
                buttonChange.performClick();
            }
        });
        textIn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    CharSequence res = code(s);
                    if (!res.equals("")) textOut.setText(res);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        info.setOnClickListener(v -> {
            AlertDialog alertDialog1 = new AlertDialog.Builder(this)
                    .setTitle(R.string.titel)//标题
                    .setMessage(R.string.content)//内容
                    .setIcon(R.mipmap.lock)//图标
                    .setCancelable(true)
                    .create();
            alertDialog1.show();
        });
        delete.setOnClickListener(v -> {
            textIn.setText("");
            textOut.setText("");
        });
        buttonChange.setOnClickListener(v -> {
            if (id.getText().toString().isEmpty()) {
                buttonChange.setText(R.string.dersa);
                encode = 3;
            }
            if (!id.getText().toString().isEmpty()) {
                if (encode == 0) {
                    buttonChange.setText(R.string.decode);
                    encode += 1;
                } else if (encode == 1) {
                    buttonChange.setText(R.string.enrsa);
                    encode += 1;
                } else {
                    buttonChange.setText(R.string.encode);
                    encode = 0;
                }
            }
            try {
                CharSequence res = code(textIn.getText());
                if (!res.equals("")) textOut.setText(res);
            } catch (Exception e) {
                textOut.setText("");
            }
        });
        buttonCopy.setOnClickListener(v -> {
            if (!textOut.getText().toString().isEmpty()) {
                if (encode == 3) {
                    if (textOut.getText().length() == 32) {
                        id.setText(textOut.getText());
                    }
                }
                try {
                    CharSequence res = textOut.getText();
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData cd = ClipData.newPlainText("Result", res);
                    cm.setPrimaryClip(cd);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "???", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonShare.setOnClickListener(v -> {
            if (!textOut.getText().toString().isEmpty()) {
                try {
                    CharSequence res = textOut.getText();
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, res.toString());
                    startActivity(Intent.createChooser(sendIntent, ""));
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "???", Toast.LENGTH_SHORT).show();
                }
            }
        });
        initKey.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = ClipData.newPlainText("Result", publicKey);
            cm.setPrimaryClip(cd);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, publicKey);
            startActivity(Intent.createChooser(sendIntent, "RSA"));
        });
        toPhoto.setOnClickListener(v -> {
            Intent intent1 = new Intent(this, PhotoActivity.class);
            startActivity(intent1);
            overridePendingTransition(R.anim.from_right, R.anim.no_slide);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        Configuration mConfiguration = getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        boolean isLand = ori == Configuration.ORIENTATION_LANDSCAPE;
        if (isLand && !isInMultiWindowMode()) {
            setContentView(R.layout.activity_main_land);
        } else {
            setContentView(R.layout.activity_main);
        }
        initView();
        if (savedInstanceState != null) {
            encode = savedInstanceState.getInt("encode");
            publicKey = savedInstanceState.getString("publicKey");
            privateKey = savedInstanceState.getString("privateKey");
            textIn.setText(savedInstanceState.getString("textIn"));
            textOut.setText(savedInstanceState.getString("textOut"));
            id.setText(savedInstanceState.getString("id"));
            buttonChange.setText(savedInstanceState.getString("mode"));
        } else {
            Map<String, Object> keyMap;
            try {
                keyMap = initKey(1024);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            publicKey = getPublicKeyStr(keyMap);
            privateKey = getPrivateKeyStr(keyMap);
            SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
            String idm = sharedPreferences.getString("id", "");
            id.setText(idm);
        }
        initViewEvents();
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_PROCESS_TEXT.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleProcessText(intent);
            }
        }
    }

    private void handleProcessText(@NonNull Intent intent) {
        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (sharedText != null) {
            textIn.setText(sharedText);
        }
    }

    public CharSequence code(@NonNull CharSequence cs) throws Exception {
        String data = cs.toString();
        if (data.isEmpty()) return "";
        String key = "", iv = "";
        if (id.getText().length() != 0) {
            key = id.getText().toString();
            iv = key.substring(0, 16);
        }
        if (encode == 0) {
            byte[] ciphertext = encryptCBC(data.getBytes(), key.getBytes(), iv.getBytes());
            return Base64.encodeToString(ciphertext, Base64.DEFAULT);
        } else if (encode == 1) {
            return new String(decryptCBC(Base64.decode(data, Base64.DEFAULT), key.getBytes(), iv.getBytes()));
        } else if (encode == 2) {
            return encryptRSA(key, textIn.getText().toString());
        } else {
            return decryptRSA(textIn.getText().toString(), privateKey);
        }
    }
}