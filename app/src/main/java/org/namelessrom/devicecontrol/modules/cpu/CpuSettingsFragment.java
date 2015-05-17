/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.namelessrom.devicecontrol.modules.cpu;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.DeviceConstants;
import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.MainActivity;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.actions.ActionProcessor;
import org.namelessrom.devicecontrol.actions.extras.MpDecisionAction;
import org.namelessrom.devicecontrol.configuration.BootupConfiguration;
import org.namelessrom.devicecontrol.configuration.ConfigConstants;
import org.namelessrom.devicecontrol.configuration.DeviceConfiguration;
import org.namelessrom.devicecontrol.hardware.GovernorUtils;
import org.namelessrom.devicecontrol.modules.cpu.monitors.CpuCoreMonitor;
import org.namelessrom.devicecontrol.objects.BootupItem;
import org.namelessrom.devicecontrol.objects.CpuCore;
import org.namelessrom.devicecontrol.objects.ShellOutput;
import org.namelessrom.devicecontrol.ui.preferences.AutoEditTextPreference;
import org.namelessrom.devicecontrol.ui.preferences.AutoSwitchPreference;
import org.namelessrom.devicecontrol.ui.views.AttachMaterialPreferenceFragment;
import org.namelessrom.devicecontrol.ui.views.CpuCoreView;
import org.namelessrom.devicecontrol.utils.DrawableHelper;
import org.namelessrom.devicecontrol.utils.PreferenceUtils;
import org.namelessrom.devicecontrol.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import alexander.martinz.libs.materialpreferences.MaterialListPreference;
import alexander.martinz.libs.materialpreferences.MaterialPreference;
import alexander.martinz.libs.materialpreferences.MaterialPreferenceCategory;
import alexander.martinz.libs.materialpreferences.MaterialSwitchPreference;

public class CpuSettingsFragment extends AttachMaterialPreferenceFragment implements ShellOutput.OnShellOutputListener,
        CpuUtils.CoreListener, CpuUtils.FrequencyListener, GovernorUtils.GovernorListener,
        MaterialPreference.MaterialPreferenceChangeListener, MaterialPreference.MaterialPreferenceClickListener {

    private MaterialListPreference mMax;
    private MaterialListPreference mMin;
    private MaterialSwitchPreference mCpuLock;

    private MaterialListPreference mGovernor;
    private MaterialPreference mGovernorTuning;
    private MaterialSwitchPreference mCpuGovLock;

    private MaterialSwitchPreference mMpDecision;
    private MaterialListPreference mCpuQuietGov;

    private static final int ID_MPDECISION = 200;
    //----------------------------------------------------------------------------------------------

    private SwitchCompat mStatusHide;
    private LinearLayout mCpuInfo;

    @Override protected int getLayoutResourceId() {
        return R.layout.preferences_cpu;
    }

    @Override protected int getFragmentId() { return DeviceConstants.ID_PERFORMANCE_CPU_SETTINGS; }

    @Override public void onResume() {
        super.onResume();
        if (mStatusHide != null && mStatusHide.isChecked()) {
            CpuCoreMonitor.getInstance(getActivity()).start(this, 1000);
        }
        CpuUtils.get().getCpuFreq(this);
        GovernorUtils.get().getGovernor(this);
    }

    @Override public void onPause() {
        super.onPause();
        CpuCoreMonitor.getInstance(getActivity()).stop();
    }

    @Override public void onCores(@NonNull final CpuUtils.Cores cores) {
        final List<CpuCore> coreList = cores.list;
        if (coreList != null && !coreList.isEmpty()) {
            final int count = coreList.size();
            for (int i = 0; i < count; i++) {
                generateRow(i, coreList.get(i));
            }
        }
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = getActivity();

        Drawable refreshDrawable = ContextCompat.getDrawable(context, R.drawable.ic_refresh);
        refreshDrawable = DrawableHelper.applyAccentColorFilter(refreshDrawable.mutate());

        final MaterialPreferenceCategory coreLimits =
                (MaterialPreferenceCategory) view.findViewById(R.id.core_limits);
        final ImageView imageViewCpu = new ImageView(context);
        imageViewCpu.setImageDrawable(refreshDrawable);
        imageViewCpu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // animate, just to make it look cool...
                rotateView(v);
                CpuUtils.get().getCpuFreq(CpuSettingsFragment.this);
            }
        });
        coreLimits.addToWidgetFrame(imageViewCpu);

        mMax = (MaterialListPreference) view.findViewById(R.id.cpu_pref_max);
        mMax.setOnPreferenceChangeListener(this);

        mMin = (MaterialListPreference) view.findViewById(R.id.cpu_pref_min);
        mMin.setOnPreferenceChangeListener(this);

        mCpuLock = (MaterialSwitchPreference) view.findViewById(R.id.cpu_pref_cpu_lock);
        mCpuLock.getSwitch().setChecked(DeviceConfiguration.get(getActivity()).perfCpuLock);
        mCpuLock.setOnPreferenceChangeListener(this);

        final MaterialPreferenceCategory governor =
                (MaterialPreferenceCategory) view.findViewById(R.id.cpu_cat_gov);
        final ImageView imageViewGov = new ImageView(context);
        imageViewGov.setImageDrawable(refreshDrawable);
        imageViewGov.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // animate, just to make it look cool...
                rotateView(v);
                GovernorUtils.get().getGovernor(CpuSettingsFragment.this);
            }
        });
        governor.addToWidgetFrame(imageViewGov);

        mGovernor = (MaterialListPreference) view.findViewById(R.id.cpu_pref_governor);
        mGovernor.setOnPreferenceChangeListener(this);

        mGovernorTuning = (MaterialPreference) view.findViewById(R.id.cpu_pref_governor_tuning);
        mGovernorTuning.setOnPreferenceClickListener(this);

        mCpuGovLock = (MaterialSwitchPreference) view.findViewById(R.id.cpu_pref_gov_lock);
        mCpuGovLock.getSwitch().setChecked(DeviceConfiguration.get(getActivity()).perfCpuGovLock);
        mCpuGovLock.setOnPreferenceChangeListener(this);

        if (Utils.fileExists(getResources().getStringArray(R.array.directories_intelli_plug))
                || Utils.fileExists(getString(R.string.directory_mako_hotplug))
                || Utils.fileExists(getString(R.string.file_cpu_quiet_base))
                || Utils.fileExists(MpDecisionAction.MPDECISION_PATH)) {
            setupHotpluggingPreferences();
        }

        // compensate mpdecision's madness
        mMax.postDelayed(new Runnable() {
            @Override public void run() {
                rotateView(imageViewCpu);
                CpuUtils.get().getCpuFreq(CpuSettingsFragment.this);
                mMin.postDelayed(new Runnable() {
                    @Override public void run() {
                        rotateView(imageViewCpu);
                        CpuUtils.get().getCpuFreq(CpuSettingsFragment.this);
                    }
                }, 300);
            }
        }, 300);
        mGovernor.postDelayed(new Runnable() {
            @Override public void run() {
                rotateView(imageViewGov);
                GovernorUtils.get().getGovernor(CpuSettingsFragment.this);
            }
        }, 300);
    }

    private void rotateView(View v) {
        if (v == null) {
            return;
        }
        v.clearAnimation();

        ObjectAnimator animator = ObjectAnimator.ofFloat(v, "rotation", 0.0f, 360.0f);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void setupHotpluggingPreferences() {
        MaterialPreferenceCategory category = null;
        AutoSwitchPreference togglePref;
        AutoEditTextPreference editPref;
        String[] paths;
        String path;

        //------------------------------------------------------------------------------------------
        // General
        //------------------------------------------------------------------------------------------
        boolean mpdecision = Utils.fileExists(MpDecisionAction.MPDECISION_PATH);
        boolean cpuQuiet = Utils.fileExists(getString(R.string.file_cpu_quiet_base))
                && Utils.fileExists(getString(R.string.file_cpu_quiet_avail_gov))
                && Utils.fileExists(getString(R.string.file_cpu_quiet_cur_gov));
        boolean hotplug = mpdecision || cpuQuiet;
        if (hotplug) {
            category = createPreferenceCategory("hotplugging", getString(R.string.hotplugging));
            addPreference(category);
        }

        if (mpdecision) {
            mMpDecision = createSwitchPreference(false, "mpdecision",
                    getString(R.string.mpdecision), getString(R.string.mpdecision_summary), false);
            category.addPreference(mMpDecision);
            Utils.getCommandResult(this, ID_MPDECISION, "pgrep mpdecision 2> /dev/null;");
        }

        if (cpuQuiet) {
            final String[] govs = Utils.readOneLine(
                    getString(R.string.file_cpu_quiet_avail_gov)).split(" ");
            final String gov = Utils.readOneLine(getString(R.string.file_cpu_quiet_cur_gov));
            mCpuQuietGov = new MaterialListPreference(getActivity());
            mCpuQuietGov.setAsCard(false);
            mCpuQuietGov.init(getActivity());
            mCpuQuietGov.setKey("pref_cpu_quiet_governor");
            mCpuQuietGov.setTitle(getString(R.string.cpu_quiet));
            mCpuQuietGov.setAdapter(mCpuQuietGov.createAdapter(govs, govs));
            mCpuQuietGov.setValue(gov);
            category.addPreference(mCpuQuietGov);
            mCpuQuietGov.setOnPreferenceChangeListener(this);
        }

        //------------------------------------------------------------------------------------------
        // Intelli-Plug
        //------------------------------------------------------------------------------------------
        paths = getResources().getStringArray(R.array.directories_intelli_plug);
        path = Utils.checkPaths(paths);
        if (!TextUtils.isEmpty(path)) {
            category = createPreferenceCategory("intelli_plug", getString(R.string.intelli_plug));
            addPreference(category);

            // setup intelli plug toggle
            if (Utils.fileExists(path + "intelli_plug_active")) {
                togglePref = new AutoSwitchPreference(getActivity());
                togglePref.setAsCard(false);
                togglePref.init(getActivity());
                togglePref.setKey("intelli_plug_intelli_plug_active");
                togglePref.setTitle(getString(R.string.enable));
                togglePref.setPath(path + "intelli_plug_active");
                togglePref.initValue();
                category.addPreference(togglePref);
            }
            // setup touch boost toggle
            if (Utils.fileExists(path + "touch_boost_active")) {
                togglePref = new AutoSwitchPreference(getActivity());
                togglePref.setAsCard(false);
                togglePref.init(getActivity());
                togglePref.setKey("intelli_plug_touch_boost_active");
                togglePref.setTitle(getString(R.string.touch_boost));
                togglePref.setPath(path + "touch_boost_active");
                togglePref.initValue();
                category.addPreference(togglePref);
            }
            // add the other files
            final String[] files = Utils.listFiles(path, false);
            for (final String file : files) {
                final int type = PreferenceUtils.getType(file);
                if (PreferenceUtils.TYPE_EDITTEXT == type) {
                    editPref = new AutoEditTextPreference(getActivity());
                    editPref.setAsCard(false);
                    editPref.init(getActivity());
                    editPref.setKey("intelli_plug_" + file);
                    editPref.setTitle(file);
                    editPref.setPath(path + file);
                    editPref.initValue();
                    category.addPreference(editPref);
                }
            }
        }

        path = Utils.checkPath(getString(R.string.directory_mako_hotplug));
        if (!TextUtils.isEmpty(path)) {
            category = createPreferenceCategory("mako_hotplug", getString(R.string.mako_hotplug));
            addPreference(category);

            // setup mako_hotplug toggle
            if (Utils.fileExists(path + "enabled")) {
                togglePref = new AutoSwitchPreference(getActivity());
                togglePref.setAsCard(false);
                togglePref.init(getActivity());
                togglePref.setKey("mako_enabled");
                togglePref.setTitle(getString(R.string.enable));
                togglePref.setPath(path + "enabled");
                togglePref.initValue();
                category.addPreference(togglePref);
            }
            final String[] files = Utils.listFiles(path, true);
            for (final String file : files) {
                final int type = PreferenceUtils.getType(file);
                if (PreferenceUtils.TYPE_EDITTEXT == type) {
                    editPref = new AutoEditTextPreference(getActivity());
                    editPref.setAsCard(false);
                    editPref.init(getActivity());
                    editPref.setKey("mako_" + file);
                    editPref.setTitle(file);
                    editPref.setPath(path + file);
                    editPref.initValue();
                    category.addPreference(editPref);
                }
            }
        }

    }

    @Override public boolean onPreferenceChanged(MaterialPreference preference, Object o) {
        if (preference == mMax) {
            final String selected = String.valueOf(o);
            final String other = String.valueOf(mMin.getValue());
            final boolean updateOther = Utils.parseInt(selected) < Utils.parseInt(other);
            if (updateOther) {
                mMin.setValue(selected);
            }

            ActionProcessor.processAction(ActionProcessor.ACTION_CPU_FREQUENCY_MAX, selected, true);
            return true;
        } else if (preference == mMin) {
            final String selected = String.valueOf(o);
            final String other = String.valueOf(mMax.getValue());
            final boolean updateOther = Utils.parseInt(selected) > Utils.parseInt(other);
            if (updateOther) {
                mMax.setValue(selected);
            }

            ActionProcessor.processAction(ActionProcessor.ACTION_CPU_FREQUENCY_MIN, selected, true);
            return true;
        } else if (preference == mCpuLock) {
            DeviceConfiguration.get(getActivity()).perfCpuLock = (Boolean) o;
            DeviceConfiguration.get(getActivity()).saveConfiguration(getActivity());
            return true;
        } else if (preference == mGovernor) {
            final String selected = String.valueOf(o);
            ActionProcessor.processAction(ActionProcessor.ACTION_CPU_GOVERNOR, selected, true);
            return true;
        } else if (preference == mCpuGovLock) {
            DeviceConfiguration.get(getActivity()).perfCpuGovLock = (Boolean) o;
            DeviceConfiguration.get(getActivity()).saveConfiguration(getActivity());
            return true;
        } else if (preference == mMpDecision) {
            final boolean value = (Boolean) o;
            new MpDecisionAction(value ? "1" : "0", true).triggerAction();
            return true;
        } else if (preference == mCpuQuietGov) {
            final String path = Application.get().getString(R.string.file_cpu_quiet_cur_gov);
            final String value = String.valueOf(o);
            Utils.runRootCommand(Utils.getWriteCommand(path, value));
            BootupConfiguration.setBootup(getActivity(), new BootupItem(
                    ConfigConstants.CATEGORY_EXTRAS, mCpuQuietGov.getKey(),
                    path, value, true));
            return true;
        }

        return false;
    }

    @Override public boolean onPreferenceClicked(MaterialPreference preference) {
        if (preference == mGovernorTuning) {
            MainActivity.loadFragment(getActivity(), DeviceConstants.ID_GOVERNOR_TUNABLE);
            return true;
        }
        return false;
    }

    public void onShellOutput(final ShellOutput shellOutput) {
        if (shellOutput != null) {
            switch (shellOutput.id) {
                case ID_MPDECISION:
                    if (mMpDecision != null) {
                        mMpDecision.setChecked(!TextUtils.isEmpty(shellOutput.output));
                        mMpDecision.setOnPreferenceChangeListener(this);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedState) {
        setHasOptionsMenu(true);

        // inflate the parent layout
        final View view = super.onCreateView(inflater, root, savedState);
        assert view != null; // we know it because we return it
        mCpuInfo = (LinearLayout) view.findViewById(R.id.cpu_info);

        mStatusHide = (SwitchCompat) view.findViewById(R.id.cpu_info_hide);
        mStatusHide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(final CompoundButton button, final boolean b) {
                if (b) {
                    mCpuInfo.setVisibility(View.VISIBLE);
                    CpuCoreMonitor.getInstance(getActivity())
                            .start(CpuSettingsFragment.this, 1000);
                } else {
                    CpuCoreMonitor.getInstance(getActivity()).stop();
                    mCpuInfo.setVisibility(View.GONE);
                }
                DeviceConfiguration.get(getActivity()).perfCpuInfo = b;
                DeviceConfiguration.get(getActivity()).saveConfiguration(getActivity());
            }
        });
        mStatusHide.setChecked(DeviceConfiguration.get(getActivity()).perfCpuInfo);
        if (mStatusHide.isChecked()) {
            mCpuInfo.setVisibility(View.VISIBLE);
        } else {
            mCpuInfo.setVisibility(View.GONE);
        }

        CpuCore tmpCore;
        final int mCpuNum = CpuUtils.get().getNumOfCpus();
        final String format = getString(R.string.core) + " %s:";
        for (int i = 0; i < mCpuNum; i++) {
            tmpCore = new CpuCore(String.format(format, String.valueOf(i)), "0", "0", "0");
            generateRow(i, tmpCore);
        }

        return view;
    }

    @Override public void onFrequency(@NonNull final CpuUtils.Frequency cpuFreq) {
        final String[] mAvailableFrequencies = cpuFreq.available;
        Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return Utils.tryValueOf(object1, 0).compareTo(Utils.tryValueOf(object2, 0));
            }
        });
        Collections.reverse(Arrays.asList(mAvailableFrequencies));

        final ArrayList<String> entries = new ArrayList<>();
        for (final String mAvailableFreq : mAvailableFrequencies) {
            entries.add(CpuUtils.toMhz(mAvailableFreq));
        }

        final String[] entryArray = entries.toArray(new String[entries.size()]);

        mMax.setAdapter(mMax.createAdapter(entryArray, mAvailableFrequencies));
        mMax.setValue(CpuUtils.toMhz(cpuFreq.maximum));
        mMax.setEnabled(true);

        mMin.setAdapter(mMin.createAdapter(entryArray, mAvailableFrequencies));
        mMin.setValue(CpuUtils.toMhz(cpuFreq.minimum));
        mMin.setEnabled(true);

        entries.clear();
    }

    @Override public void onGovernor(@NonNull final GovernorUtils.Governor governor) {
        mGovernor.setAdapter(mGovernor.createAdapter(governor.available, governor.available));
        mGovernor.setValue(governor.current);
        mGovernor.setEnabled(true);
    }

    public View generateRow(final int core, final CpuCore cpuCore) {
        if (!isAdded() || mCpuInfo == null) { return null; }
        Logger.v(this, String.format("generateRow(%s);", cpuCore.toString()));

        View rowView = mCpuInfo.getChildAt(core);
        if (rowView == null) {
            rowView = new CpuCoreView(getActivity());
            mCpuInfo.addView(rowView);
        }

        if (rowView instanceof CpuCoreView) {
            final boolean isOffline = cpuCore.mCoreCurrent == 0;

            ((CpuCoreView) rowView).core.setText(cpuCore.mCore);
            ((CpuCoreView) rowView).freq.setText(isOffline
                    ? getString(R.string.core_offline)
                    : CpuUtils.toMhz(String.valueOf(cpuCore.mCoreCurrent))
                    + " / " + CpuUtils.toMhz(String.valueOf(cpuCore.mCoreMax))
                    + " [" + cpuCore.mCoreGov + ']');
            ((CpuCoreView) rowView).bar.setMax(cpuCore.mCoreMax);
            ((CpuCoreView) rowView).bar.setProgress(cpuCore.mCoreCurrent);
        }

        return rowView;
    }

}

