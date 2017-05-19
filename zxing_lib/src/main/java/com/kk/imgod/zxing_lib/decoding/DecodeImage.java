package com.kk.imgod.zxing_lib.decoding;

import android.graphics.BitmapFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;
import java.util.Vector;

public class DecodeImage {
    private static MultiFormatReader multiFormatReader;
    private static Hashtable<DecodeHintType, Object> hints;

    private static void initHints() {
        multiFormatReader = new MultiFormatReader();
        hints = new Hashtable<>(2);

        Vector<BarcodeFormat> decodeFormats = new Vector<>();
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

//        hints.put(DecodeHintType.CHARACTER_SET, "UTF8");

        multiFormatReader.setHints(hints);
    }

    public static Result readImage(String path) {
        initHints();
        Result rawResult = null;
        try {
            rawResult = multiFormatReader
                    .decodeWithState(new BinaryBitmap(new HybridBinarizer(
                            new BitmapLuminanceSource(BitmapFactory
                                    .decodeFile(path)))));
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return rawResult;
    }
}
