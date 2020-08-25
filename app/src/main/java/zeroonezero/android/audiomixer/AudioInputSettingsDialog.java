package zeroonezero.android.audiomixer;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;

import java.util.Formatter;
import java.util.Locale;

public class AudioInputSettingsDialog extends Dialog {
    private Input input;

    private TextView trimMinTextView, trimMaxTextView;

    public AudioInputSettingsDialog(Context context, Input input) {
        super(context);
        this.input = input;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.audio_input_settings_dialog);


        trimMinTextView = findViewById(R.id.trim_min_text_view);
        trimMaxTextView = findViewById(R.id.trim_max_text_view);
        RangeSeekBar<Float> trimRangeBar = findViewById(R.id.trim_range_bar);
        trimRangeBar.setRangeValues(0f, 1f);
        trimRangeBar.setNotifyWhileDragging(true);
        trimRangeBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Float>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Float minValue, Float maxValue) {
                setTrim(minValue, maxValue);
            }
        });
        trimRangeBar.setSelectedMinValue(0f);
        trimRangeBar.setSelectedMaxValue(1f);
        setTrim(0f, 1f);

        Button trimViewCrossButton = findViewById(R.id.done);
        trimViewCrossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    float offset = Float.parseFloat(((EditText)findViewById(R.id.start_time_edit_text)).getText().toString());
                    input.startOffsetUs = (long)(offset * 1000000);
                }catch (Exception e){

                }
                dismiss();
            }
        });

    }

    private void setTrim(float minValue, float maxValue){
        input.startTimeUs = (long)(minValue * input.durationUs);
        input.endTimeUs = (long)(maxValue * input.durationUs);

        String startTime = stringForDuration((long)(minValue * input.durationUs) );
        String endTime = stringForDuration((long)(maxValue * input.durationUs) );

        trimMinTextView.setText(startTime);
        trimMaxTextView.setText(endTime);
    }

    public static String stringForDuration(long timeUs) {
        int timeMs = (int)(timeUs / 1000);

        StringBuilder mFormatBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;
        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}