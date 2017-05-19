package com.kk.imgod.zxing_lib.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * Class Name : ImageUtils.
 * Description : TODO
 * Created by imgod.
 * Create Date 2017/5/19 13:09.
 */

public class ImageUtils {
    /**
     * 拍照获得图片
     * 传入的路径就是图片的路径
     *
     * @param activity    Activity对象
     * @param imgPath     文件的路径
     * @param requestCode 请求码
     */
    public static void takePhotoFromCamera(Activity activity, String imgPath, int requestCode) {
        Intent intent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imgUri = Uri.fromFile(new File(imgPath));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 从相册获得图片
     * 首先在回调中拿到uri,根据uri通过方法.拿到路径
     * String imgPath = ImageUtils.getImageAbsolutePath(mContext, data.getData());
     *
     * @param activity    活动
     * @param requestCode 请求码
     */
    public static void takePhotoFromAbnum(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
     *
     * @param context
     * @param imageUri
     * @author yaoxing
     * @date 2016年4月6日
     */
    public static String getImageAbsolutePath(Activity context, Uri imageUri) {
        if (context.getPackageManager().checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            if (context == null || imageUri == null)
                return null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
                if (isExternalStorageDocument(imageUri)) {
                    String docId = DocumentsContract.getDocumentId(imageUri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(imageUri)) {
                    String id = DocumentsContract.getDocumentId(imageUri);
                    if (!TextUtils.isEmpty(id)) {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                        return getDataColumn(context, contentUri, null, null);
                    }
                } else if (isMediaDocument(imageUri)) {
                    String docId = DocumentsContract.getDocumentId(imageUri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    String selection = MediaStore.Images.Media._ID + "=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } // MediaStore (and general)
            else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
                // Return the remote address
                if (isGooglePhotosUri(imageUri))
                    return imageUri.getLastPathSegment();
                return getDataColumn(context, imageUri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
                return imageUri.getPath();
            }
        } else {
            Log.e("imageUtils", "没有读取文件系统的权限,图片路径获取失败");
        }
        return null;
    }


    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
