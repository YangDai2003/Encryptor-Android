package com.example.encryptor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static String getRealPathFromURI(Context mContext, Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }
    public static void shareFile(Context context, String path) {
        File file = new File(path);
        if (file.exists()) {
            Intent share = new Intent(Intent.ACTION_SEND);
            Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setType("text/plain");
            share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(share, ""));
        } else {
            Toast.makeText(context, R.string.error2, Toast.LENGTH_SHORT).show();
        }
    }
    public static String getFileFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        switch (uri.getScheme()) {
            case ContentResolver.SCHEME_CONTENT:
                //Android7.0之后的uri content:// URI
                return getFilePathFromContentUri(context, uri);
            case ContentResolver.SCHEME_FILE:
            default:
                //Android7.0之前的uri file://
                return new File(uri.getPath()).getAbsolutePath();
        }
    }
    /**
     * 从uri获取path
     * FileProvider适配
     * content://com.tencent.mobileqq.fileprovider/external_files/storage/emulated/0/Tencent/QQfile_recv/
     * content://com.tencent.mm.external.fileprovider/external/tencent/MicroMsg/Download/
     */
    private static String getFilePathFromContentUri(Context context, Uri uri) {
        if (null == uri) return null;
        String data = null;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (index > -1) {
                    data = cursor.getString(index);
                } else {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    String fileName = cursor.getString(nameIndex);
                    data = getPathFromInputStreamUri(context, uri, fileName);
                }
            }
            cursor.close();
        }
        return data;
    }

    /**
     * 用流拷贝文件一份到自己APP私有目录下
     */
    private static String getPathFromInputStreamUri(Context context, Uri uri, String fileName) {
        InputStream inputStream = null;
        String filePath = null;
        if (uri.getAuthority() != null) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                File file = createTemporalFileFrom(context, inputStream, fileName);
                filePath = file.getPath();

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return filePath;
    }

    private static File createTemporalFileFrom(Context context, InputStream inputStream, String fileName) throws IOException {
        File targetFile = null;
        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[8 * 1024];
            //自己定义拷贝文件路径
            targetFile = new File(context.getExternalCacheDir(), fileName);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            OutputStream outputStream = new FileOutputStream(targetFile);

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return targetFile;
    }
}
