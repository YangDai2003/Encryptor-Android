package com.example.encryptor;

import static android.content.Context.MODE_PRIVATE;
import static com.example.encryptor.CryptologyUtils.decryptCBC;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class FragmentDe extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    String text = "";
    View view;
    TextView path;
    Button confirm, share;
    ImageView imageView;
    Uri out;
    String key, iv;
    Context mContext;

    public FragmentDe() {
        // Required empty public constructor
    }

    protected void initView() {
        path = view.findViewById(R.id.textView2);
        confirm = view.findViewById(R.id.button11);
        share = view.findViewById(R.id.button10);
        imageView = view.findViewById(R.id.imageView);
    }

    @SuppressLint("SetTextI18n")
    protected void initViewEvents() {
        share.setOnClickListener(v -> {
            if (out != null) {
                shareImage();
            }
        });
        imageView.setOnClickListener(i -> {
            if (imageView.getDrawable() != null) {
                bigImageLoader(((BitmapDrawable) imageView.getDrawable()).getBitmap());
            }
        });

        confirm.setOnClickListener(v -> new Thread(() -> {
            Bitmap bitmap = null;
            if (!path.getText().toString().isEmpty()) {
                try {
                    bitmap = bytesToBitmap(decryptCBC(Base64.decode(String.valueOf(load()), Base64.DEFAULT), key.getBytes(), iv.getBytes()));
                } catch (Exception ignored) {
                }
            }
            Bitmap finalBitmap = bitmap;
            requireActivity().runOnUiThread(() -> {
                if (!path.getText().toString().isEmpty()) {
                    try {
                        imageView.setImageBitmap(finalBitmap);
                        assert finalBitmap != null;
                        saveImage(finalBitmap);
                    } catch (Exception e) {
                        Toast.makeText(mContext, "???", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }).start());
    }

    private void shareImage() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, out);
        startActivity(Intent.createChooser(intent, ""));
    }

    /**
     * API29 中的最新保存图片到相册的方法
     */
    private void saveImage(@NonNull Bitmap toBitmap) {
        //开始一个新的进程执行保存图片的操作
        out = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        //使用use可以自动关闭流
        try {
            OutputStream outputStream = mContext.getContentResolver().openOutputStream(out, "rw");
            if (toBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                Toast.makeText(mContext, R.string.save, Toast.LENGTH_SHORT).show();
            } else {
                Log.e("保存失败", "fail");
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(mContext, R.string.error2, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从文件中加载
     */
    private StringBuilder load() {
        File file = new File(path.getText().toString());
        InputStream fis = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            String str;
            while ((str = reader.readLine()) != null) {
                content.append(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content;
    }

    private void bigImageLoader(Bitmap bitmap) {
        final Dialog dialog = new Dialog(mContext);
        ImageView image = new ImageView(mContext);
        image.setImageBitmap(bitmap);
        dialog.setContentView(image);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        image.setOnClickListener(v -> dialog.cancel());
    }

    private Bitmap bytesToBitmap(byte[] bytes) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static FragmentDe newInstance(String param1) {
        FragmentDe fragment = new FragmentDe();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user", MODE_PRIVATE);
        String idm = sharedPreferences.getString("id", "");
        if (!idm.isEmpty()) {
            key = idm;
            iv = key.substring(0, 16);
        }
        mContext = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_de, container, false);
        initView();
        if (getArguments() != null) {
            if (null != getArguments().getString(ARG_PARAM1))
                text = getArguments().getString(ARG_PARAM1);
        }
        if (!text.isEmpty()) path.setText(text);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewEvents();
        ((PhotoActivity) requireActivity()).setOnDataChange(data -> path.setText(data));
    }
}