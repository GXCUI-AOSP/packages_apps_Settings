/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.internal.os.storage.ExternalStorageFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Confirm and execute a format of the sdcard.
 * Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE SD CARD" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 */
public class MediaFormat extends Activity {

    private static final int KEYGUARD_REQUEST = 55;

    private LayoutInflater mInflater;

    private View mInitialView;
    private Button mInitiateButton;
    private CheckBox mAdvancedFormat;
    private TextView mFormatTitle;
    private RadioGroup mRadioGroup;

    private View mFinalView;
    private Button mFinalButton;

    private List<String> mkfs = new ArrayList<String>();

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Mount Service to format the SD card.
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {
            public void onClick(View v) {

                if (Utils.isMonkeyRunning()) {
                    return;
                }
                Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
                intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                // Transfer the storage volume to the new intent
                final StorageVolume storageVolume = getIntent().getParcelableExtra(
                        StorageVolume.EXTRA_STORAGE_VOLUME);
                intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, storageVolume);

                if (mRadioGroup.getCheckedRadioButtonId() != -1) {
                    RadioButton button = (RadioButton)mInitialView.
                            findViewById(mRadioGroup.getCheckedRadioButtonId());
                    intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME_FS, button.getText());
                }

                startService(intent);
                finish();
            }
        };

    /**
     *  Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     */
    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(this)
                .launchConfirmationActivity(request,
                        getText(R.string.media_format_gesture_prompt),
                        getText(R.string.media_format_gesture_explanation));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            establishFinalConfirmationState();
        } else if (resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else {
            establishInitialState();
        }
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private Button.OnClickListener mInitiateListener = new Button.OnClickListener() {
            public void onClick(View v) {
                if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                    establishFinalConfirmationState();
                }
            }
        };


    private CheckBox.OnClickListener mAdvancedSettings = new CheckBox.OnClickListener() {
            public void onClick(View v) {
                mFormatTitle.setVisibility(mAdvancedFormat.isChecked() ?
                        View.VISIBLE : View.INVISIBLE);
                mRadioGroup.setVisibility(mAdvancedFormat.isChecked() ?
                        View.VISIBLE : View.INVISIBLE);
            }
        };


    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        if (mFinalView == null) {
            mFinalView = mInflater.inflate(R.layout.media_format_final, null);
            mFinalButton =
                    (Button) mFinalView.findViewById(R.id.execute_media_format);
            mFinalButton.setOnClickListener(mFinalClickListener);
        }

        setContentView(mFinalView);
    }

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        if (mInitialView == null) {
            mInitialView = mInflater.inflate(R.layout.media_format_primary, null);
            mInitiateButton =
                    (Button) mInitialView.findViewById(R.id.initiate_media_format);
            mFormatTitle = (TextView)
                    mInitialView.findViewById(R.id.media_format_title);
            mRadioGroup = (RadioGroup)
                    mInitialView.findViewById(R.id.media_format_supported_fs);
            mAdvancedFormat = (CheckBox)
                    mInitialView.findViewById(R.id.media_format_advanced);

            mFormatTitle.setVisibility(mAdvancedFormat.isChecked() ?
                    View.VISIBLE : View.INVISIBLE);
            mRadioGroup.setVisibility(mAdvancedFormat.isChecked() ?
                    View.VISIBLE : View.INVISIBLE);

            mInitiateButton.setOnClickListener(mInitiateListener);
            mAdvancedFormat.setOnClickListener(mAdvancedSettings);

            getSupportedFilesystems();
        }

        setContentView(mInitialView);
    }

    private void getSupportedFilesystems() {
        String paths[] = {"/system/bin/", "/system/xbin/"};
        File[] fileList;
        mkfs.clear();

        for (int i = 0; i < paths.length; i++) {
            fileList = new File(paths[i]).listFiles();
            for (int j = 0; j < fileList.length; j++) {
                if (fileList[j].isFile() && fileList[j].getName().contains("mkfs")) {
                    mkfs.add(fileList[j].getName().split("mkfs.")[1]);
                    // in the case of mkfs.ext2/3, we actually want ext4. ninja it!
                    if (mkfs.get(mkfs.size() - 1).equals("ext2") ||
                            mkfs.get(mkfs.size() - 1).equals("ext3"))
                        mkfs.set(mkfs.size() - 1, "ext4");
                }
            }
        }

        // Remove any duplicates
        mkfs = new ArrayList<String>(new LinkedHashSet<String>(mkfs));

        for (int i = 0; i < mkfs.size(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(mkfs.get(i));
            radioButton.setId(i);
            mRadioGroup.addView(radioButton);
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mInitialView = null;
        mFinalView = null;
        mInflater = LayoutInflater.from(this);

        establishInitialState();
    }

    /** Abandon all progress through the confirmation sequence by returning
     * to the initial view any time the activity is interrupted (e.g. by
     * idle timeout).
     */
    @Override
    public void onPause() {
        super.onPause();

        if (!isFinishing()) {
            establishInitialState();
        }
    }
}
