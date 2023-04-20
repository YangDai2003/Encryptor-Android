package com.example.encryptor;

import static android.content.Context.MODE_PRIVATE;

import static com.example.encryptor.CryptologyUtils.encryptCBC;
import static com.example.encryptor.FileUtils.getRealPathFromURI;
import static com.example.encryptor.FileUtils.shareFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FragmentEn extends Fragment {

    private static final String ARG_PARAM1 = "param1";

    Context mContext;
    View view;
    Button picker, confirm, share;
    ImageView imageView;
    TextView path;
    ActivityResultLauncher<Intent> intentActivityResultLauncher;
    Uri uri;
    String key, iv;

    public FragmentEn() {
        // Required empty public constructor
    }

    protected void initView() {
        picker = view.findViewById(R.id.button6);
        imageView = view.findViewById(R.id.imageView);
        confirm = view.findViewById(R.id.button11);
        share = view.findViewById(R.id.button10);
        path = view.findViewById(R.id.textView2);
    }

    @SuppressLint("SetTextI18n")
    protected void initViewEvents() {
        share.setOnClickListener(v -> {
            if (!path.getText().toString().isEmpty()) {
                shareFile(mContext, requireActivity().getFilesDir().toString() + "/imagetxt.txt");
            }
        });
        imageView.setOnClickListener(i -> {
            if (imageView.getDrawable() != null) {
                bigImageLoader(((BitmapDrawable) imageView.getDrawable()).getBitmap());
            }
        });
        picker.setOnClickListener(v -> {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                intent.setType("image/*");
                intentActivityResultLauncher.launch(intent);
            } else {
                intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                intentActivityResultLauncher.launch(intent);
            }
            path.setText("");
        });
        confirm.setOnClickListener(v -> new Thread(() -> {
            if (imageView.getDrawable() != null) {
                try {
                    byte[] bt = encodeImage(((BitmapDrawable) imageView.getDrawable()).getBitmap());
                    saveFile(Base64.encodeToString(encryptCBC(bt, key.getBytes(), iv.getBytes()), Base64.DEFAULT));
                } catch (Exception e) {
                    Toast.makeText(mContext, R.string.error2, Toast.LENGTH_SHORT).show();
                }
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(mContext, R.string.success, Toast.LENGTH_SHORT).show();
                    path.setText(requireActivity().getFilesDir().getAbsolutePath() + "/imagetxt.txt");
                });
            }
        }).start());
    }

    public Bitmap decodeSampledBitmap(String path) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        // Calculate inSampleSize
        DisplayMetrics metric = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        int height = metric.heightPixels;   // 屏幕高度（像素）
        options.inSampleSize = calculateInSampleSize(options, width, height);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            inSampleSize *= 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /***
     * 保存到文件
     */
    public void saveFile(String str) {
        FileOutputStream fos = null;
        BufferedWriter writer = null;
        try {
            fos = requireActivity().openFileOutput("imagetxt.txt", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fos));
            try {
                writer.write(str);
            } catch (IOException ignored) {
            }
        } catch (FileNotFoundException ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

    public byte[] encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream it = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it); //参数如果为100那么就不压缩
        return it.toByteArray();
    }

    public static FragmentEn newInstance(String param1) {
        FragmentEn fragment = new FragmentEn();
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
        intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //此处是跳转的result回调方法
            new Thread(() -> {
                Bitmap bitmap;
                if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
                    uri = result.getData().getData();
                    bitmap = decodeSampledBitmap(getRealPathFromURI(mContext, uri));
                    Bitmap finalBitmap1 = bitmap;
                    requireActivity().runOnUiThread(() -> imageView.setImageBitmap(finalBitmap1));
                }
            }).start();
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_en, container, false);
        initView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewEvents();
    }
}