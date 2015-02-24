package me.vickychijwani.spectre.network;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import in.uncod.android.bypass.Bypass;
import me.vickychijwani.spectre.util.AppUtils;

// credits: http://stackoverflow.com/a/25530488/504611
public class PicassoImageGetter implements Bypass.ImageGetter {

    private final String baseUrl;
    private final Resources resources;
    private final Picasso pablo;
    private final TextView textView;
    private int maxWidth = -1;

    public PicassoImageGetter(final String baseUrl, final TextView textView,
                              final Resources resources, final Picasso pablo) {
        this.baseUrl = baseUrl;
        this.textView = textView;
        this.resources = resources;
        this.pablo = pablo;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (! source.startsWith("http")) {
            source = AppUtils.pathJoin(baseUrl, source);
        }

        final BitmapDrawablePlaceHolder result = new BitmapDrawablePlaceHolder();

        final String finalSource = source;
        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(final Void... meh) {
                try {
                    return pablo.load(finalSource).get();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                try {
                    if (maxWidth == -1) {
                        int horizontalPadding = textView.getPaddingLeft() + textView.getPaddingRight();
                        maxWidth = textView.getMeasuredWidth() - horizontalPadding;
                        if (maxWidth == 0) {
                            maxWidth = Integer.MAX_VALUE;
                        }
                    }

                    final BitmapDrawable drawable = new BitmapDrawable(resources, bitmap);
                    final double aspectRatio = 1.0 * drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
                    final int width = Math.min(maxWidth, drawable.getIntrinsicWidth());
                    final int height = (int) (width / aspectRatio);

                    drawable.setBounds(0, 0, width, height);

                    result.setDrawable(drawable);
                    result.setBounds(0, 0, width, height);

                    textView.setText(textView.getText()); // invalidate() doesn't work correctly...
                } catch (Exception e) {
                    /* nom nom nom */
                }
            }

        }.execute((Void) null);

        return result;
    }

    static class BitmapDrawablePlaceHolder extends BitmapDrawable {

        protected Drawable drawable;

        @Override
        public void draw(final Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

    }

}
