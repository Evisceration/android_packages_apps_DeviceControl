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
package org.namelessrom.devicecontrol;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.pollfish.constants.Position;
import com.pollfish.main.PollFish;
import com.stericson.roottools.RootTools;

import org.namelessrom.devicecontrol.activities.BaseActivity;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.listeners.OnBackPressedListener;
import org.namelessrom.devicecontrol.ui.adapters.MenuListArrayAdapter;
import org.namelessrom.devicecontrol.ui.fragments.about.AboutFragment;
import org.namelessrom.devicecontrol.ui.fragments.device.DeviceFragment;
import org.namelessrom.devicecontrol.ui.fragments.device.DeviceInformationFragment;
import org.namelessrom.devicecontrol.ui.fragments.device.sub.FastChargeFragment;
import org.namelessrom.devicecontrol.ui.fragments.device.sub.SoundControlFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.CpuSettingsFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.ExtrasFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.GpuSettingsFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.InformationFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.EntropyFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.FilesystemFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.GovernorFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.KsmFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.ThermalFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.UksmFragment;
import org.namelessrom.devicecontrol.ui.fragments.performance.sub.VoltageFragment;
import org.namelessrom.devicecontrol.ui.fragments.preferences.PreferencesFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.AppListFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.FlasherFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.TaskerFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.ToolsMoreFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.WirelessFileManagerFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.editor.BuildPropEditorFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.editor.BuildPropFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.editor.SysctlEditorFragment;
import org.namelessrom.devicecontrol.ui.fragments.tools.editor.SysctlFragment;
import org.namelessrom.devicecontrol.utils.AppHelper;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.proprietary.Configuration;

import java.io.File;

public class MainActivity extends BaseActivity implements DeviceConstants,
        AdapterView.OnItemClickListener, View.OnClickListener {

    //==============================================================================================
    // Fields
    //==============================================================================================
    private static final Object lockObject = new Object();

    private static long mBackPressed;
    private Toast mToast;

    public static SlidingMenu sSlidingMenu;
    public static MaterialMenuIconToolbar sMaterialMenu;

    public static boolean sDisableFragmentAnimations;

    private Fragment mCurrentFragment;

    private int mTitle = R.string.home;
    private int mFragmentTitle = R.string.home;
    private int mSubFragmentTitle = -1;

    private static final int[] MENU_ENTRIES = {
            R.string.device,        // Device
            ID_DEVICE_INFORMATION,
            ID_FEATURES,
            R.string.performance,   // Performance
            ID_PERFORMANCE_INFO,
            ID_PERFORMANCE_CPU_SETTINGS,
            ID_PERFORMANCE_GPU_SETTINGS,
            ID_PERFORMANCE_EXTRA,
            R.string.tools,         // Tools
            ID_TOOLS_TASKER,
            ID_TOOLS_FLASHER,
            ID_TOOLS_MORE
    };

    private static final int[] MENU_ICONS = {
            -1, // Device
            R.drawable.ic_menu_device,
            R.drawable.ic_menu_features,
            -1, // Performance
            R.drawable.ic_menu_perf_info,
            R.drawable.ic_menu_perf_cpu,
            R.drawable.ic_menu_perf_gpu,
            R.drawable.ic_menu_perf_extras,
            -1, // Tools
            R.drawable.ic_menu_tasker,
            R.drawable.ic_menu_flash,
            R.drawable.ic_menu_code
    };

    //==============================================================================================
    // Overridden Methods
    //==============================================================================================

    @Override protected void onResume() {
        super.onResume();
        final String pollFishApiKey = Configuration.getPollfishApiKeyDc();
        if (!TextUtils.equals("---", pollFishApiKey)
                && PreferenceHelper.getBoolean("show_pollfish", false)) {
            Logger.v(this, "PollFish.init()");
            PollFish.init(this, pollFishApiKey, Position.BOTTOM_RIGHT, 30);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup action bar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mSubFragmentTitle == -1) {
                    sSlidingMenu.toggle(true);
                } else {
                    onCustomBackPressed(true);
                }
            }
        });

        // setup material menu icon
        sMaterialMenu =
                new MaterialMenuIconToolbar(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN) {
                    @Override public int getToolbarViewId() { return R.id.toolbar; }
                };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sMaterialMenu.setNeverDrawTouch(true);
        }

        Utils.setupDirectories();

        final FrameLayout container = (FrameLayout) findViewById(R.id.container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            container.setBackground(null);
        } else {
            //noinspection deprecation
            container.setBackgroundDrawable(null);
        }

        final View v = getLayoutInflater().inflate(R.layout.menu_list, container, false);
        final ListView menuList = (ListView) v.findViewById(R.id.navbarlist);
        final LinearLayout menuContainer = (LinearLayout) v.findViewById(R.id.menu_container);
        // setup our static items
        menuContainer.findViewById(R.id.menu_prefs).setOnClickListener(this);
        menuContainer.findViewById(R.id.menu_about).setOnClickListener(this);

        sSlidingMenu = new SlidingMenu(this);
        sSlidingMenu.setMode(SlidingMenu.LEFT);
        sSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        sSlidingMenu.setShadowDrawable(R.drawable.shadow_left);
        sSlidingMenu.setBehindWidthRes(R.dimen.slidingmenu_offset);
        sSlidingMenu.setFadeEnabled(true);
        sSlidingMenu.setFadeDegree(0.45f);
        sSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        sSlidingMenu.setMenu(v);

        // setup touch mode
        MainActivity.setSwipeOnContent(PreferenceHelper.getBoolean("swipe_on_content", false));

        final MenuListArrayAdapter mAdapter = new MenuListArrayAdapter(
                this,
                R.layout.menu_main_list_item,
                MENU_ENTRIES,
                MENU_ICONS);
        menuList.setAdapter(mAdapter);
        menuList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        menuList.setOnItemClickListener(this);

        loadFragmentPrivate(ID_ABOUT, false);
        getSupportFragmentManager().executePendingTransactions();

        Utils.startTaskerService();

        final String downgradePath = getFilesDir() + DC_DOWNGRADE;
        if (Utils.fileExists(downgradePath)) {
            if (!new File(downgradePath).delete()) {
                Logger.wtf(this, "Could not delete downgrade indicator file!");
            }
            Toast.makeText(this, R.string.downgraded, Toast.LENGTH_LONG).show();
        }

        if (PreferenceHelper.getBoolean(DC_FIRST_START, true)) {
            PreferenceHelper.setBoolean(DC_FIRST_START, false);
        }
    }

    @Override public void onClick(final View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.menu_prefs:
                loadFragmentPrivate(ID_PREFERENCES, false);
                break;
            case R.id.menu_about:
                loadFragmentPrivate(ID_ABOUT, false);
                break;
        }
    }

    @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        loadFragmentPrivate((Integer) adapterView.getItemAtPosition(i), false);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        sMaterialMenu.syncState(savedInstanceState);
    }

    @Override protected void onSaveInstanceState(@NonNull final Bundle outState) {
        sMaterialMenu.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void onCustomBackPressed(final boolean animatePressed) {
        Logger.v(this, "onCustomBackPressed(%s)", animatePressed);

        // toggle menu if it is showing and return
        if (sSlidingMenu.isMenuShowing()) {
            sSlidingMenu.toggle(true);
            return;
        }

        // if we have a OnBackPressedListener at the fragment, go in
        if (mCurrentFragment instanceof OnBackPressedListener) {
            final OnBackPressedListener listener = ((OnBackPressedListener) mCurrentFragment);

            // if our listener handles onBackPressed(), return
            if (listener.onBackPressed()) {
                Logger.v(this, "onBackPressed()");
                return;
            }

            // else we will have to go back or exit.
            // in this case, lets get the correct icons
            final MaterialMenuDrawable.IconState iconState;
            if (listener.showBurger()) {
                iconState = MaterialMenuDrawable.IconState.BURGER;
            } else {
                iconState = MaterialMenuDrawable.IconState.ARROW;
            }

            Logger.v(this, "iconState: %s", iconState);

            // we can separate actionbar back actions and back key presses
            if (animatePressed) {
                sMaterialMenu.animatePressedState(iconState);
            } else {
                sMaterialMenu.animateState(iconState);
            }

            // after animating, go further
        }

        // we we have at least one fragment in the BackStack, pop it and return
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();

            // restore title / actionbar
            if (mSubFragmentTitle != -1) {
                mTitle = mSubFragmentTitle;
            } else {
                mTitle = mFragmentTitle;
            }
            restoreActionBar();

            return;
        }

        // if nothing matched by now, we do not have any fragments in the BackStack, nor we have
        // the menu open. in that case lets detect a double back press and exit the activity
        if (mBackPressed + 2000 > System.currentTimeMillis()) {
            if (mToast != null) { mToast.cancel(); }
            finish();
        } else {
            mToast = Toast.makeText(getBaseContext(),
                    getString(R.string.action_press_again), Toast.LENGTH_SHORT);
            mToast.show();
        }
        mBackPressed = System.currentTimeMillis();
    }

    @Override public void onBackPressed() {
        onCustomBackPressed(false);
    }

    @Override protected void onDestroy() {
        DatabaseHandler.tearDown();
        synchronized (lockObject) {
            Logger.i(this, "closing shells");
            try {
                RootTools.closeAllShells();
            } catch (Exception e) {
                Logger.e(this, String.format("onDestroy(): %s", e));
            }
        }
        super.onDestroy();
    }

    //==============================================================================================
    // Methods
    //==============================================================================================

    public void setFragment(final Fragment fragment) {
        if (fragment == null) return;
        Logger.v(this, "setFragment: %s", fragment.getId());
        mCurrentFragment = fragment;
    }

    public static void loadFragment(final Activity activity, final int id) {
        loadFragment(activity, id, false);
    }

    public static void loadFragment(final Activity activity, final int id, final boolean onResume) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).loadFragmentPrivate(id, onResume);
        }
    }

    private void loadFragmentPrivate(final int i, final boolean onResume) {
        switch (i) {
            default: // slip through...
                //--------------------------------------------------------------------------------------
            case ID_ABOUT:
                if (!onResume) mCurrentFragment = new AboutFragment();
                mTitle = mFragmentTitle = R.string.app_name;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_DEVICE_INFORMATION:
                if (!onResume) mCurrentFragment = new DeviceInformationFragment();
                mTitle = mFragmentTitle = R.string.device;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_FEATURES:
                if (!onResume) mCurrentFragment = new DeviceFragment();
                mTitle = mFragmentTitle = R.string.features;
                mSubFragmentTitle = -1;
                break;
            case ID_FAST_CHARGE:
                if (!onResume) mCurrentFragment = new FastChargeFragment();
                mTitle = mSubFragmentTitle = R.string.fast_charge;
                break;
            case ID_SOUND_CONTROL:
                if (!onResume) mCurrentFragment = new SoundControlFragment();
                mTitle = mSubFragmentTitle = R.string.sound_control;
                break;
            //--------------------------------------------------------------------------------------
            case ID_PERFORMANCE_INFO:
                if (!onResume) mCurrentFragment = new InformationFragment();
                mTitle = mFragmentTitle = R.string.information;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_PERFORMANCE_CPU_SETTINGS:
                if (!onResume) mCurrentFragment = new CpuSettingsFragment();
                mTitle = mFragmentTitle = R.string.cpusettings;
                mSubFragmentTitle = -1;
                break;
            case ID_GOVERNOR_TUNABLE:
                if (!onResume) mCurrentFragment = new GovernorFragment();
                mTitle = mSubFragmentTitle = R.string.cpu_governor_tuning;
                break;
            //--------------------------------------------------------------------------------------
            case ID_PERFORMANCE_GPU_SETTINGS:
                if (!onResume) mCurrentFragment = new GpuSettingsFragment();
                mTitle = mFragmentTitle = R.string.gpusettings;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_PERFORMANCE_EXTRA:
                if (!onResume) mCurrentFragment = new ExtrasFragment();
                mTitle = mFragmentTitle = R.string.extras;
                mSubFragmentTitle = -1;
                break;
            case ID_KSM:
                if (!onResume) mCurrentFragment = new KsmFragment();
                mTitle = mSubFragmentTitle = R.string.ksm;
                break;
            case ID_UKSM:
                if (!onResume) mCurrentFragment = new UksmFragment();
                mTitle = mSubFragmentTitle = R.string.uksm;
                break;
            case ID_THERMAL:
                if (!onResume) mCurrentFragment = new ThermalFragment();
                mTitle = mSubFragmentTitle = R.string.thermal;
                break;
            case ID_VOLTAGE:
                if (!onResume) mCurrentFragment = new VoltageFragment();
                mTitle = mSubFragmentTitle = R.string.voltage_control;
                break;
            case ID_ENTROPY:
                if (!onResume) mCurrentFragment = new EntropyFragment();
                mTitle = mSubFragmentTitle = R.string.entropy;
                break;
            case ID_FILESYSTEM:
                if (!onResume) mCurrentFragment = new FilesystemFragment();
                mTitle = mSubFragmentTitle = R.string.filesystem;
                break;
            //--------------------------------------------------------------------------------------
            case ID_TOOLS_TASKER:
                if (!onResume) mCurrentFragment = new TaskerFragment();
                mTitle = mFragmentTitle = R.string.tasker;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_TOOLS_FLASHER:
                if (!onResume) mCurrentFragment = new FlasherFragment();
                mTitle = mFragmentTitle = R.string.flasher;
                mSubFragmentTitle = -1;
                break;
            //--------------------------------------------------------------------------------------
            case ID_TOOLS_MORE:
                if (!onResume) mCurrentFragment = new ToolsMoreFragment();
                mTitle = mFragmentTitle = R.string.more;
                mSubFragmentTitle = -1;
                break;
            case ID_TOOLS_VM:
                if (!onResume) mCurrentFragment = new SysctlFragment();
                mTitle = mSubFragmentTitle = R.string.sysctl_vm;
                break;
            case ID_TOOLS_EDITORS_VM:
                if (!onResume) mCurrentFragment = new SysctlEditorFragment();
                mTitle = mSubFragmentTitle = R.string.sysctl_vm;
                break;
            case ID_TOOLS_BUILD_PROP:
                if (!onResume) mCurrentFragment = new BuildPropFragment();
                mTitle = mSubFragmentTitle = R.string.buildprop;
                break;
            case ID_TOOLS_EDITORS_BUILD_PROP:
                if (!onResume) mCurrentFragment = new BuildPropEditorFragment();
                mTitle = mSubFragmentTitle = R.string.buildprop;
                break;
            case ID_TOOLS_APP_MANAGER:
                if (!onResume) mCurrentFragment = new AppListFragment();
                mTitle = mSubFragmentTitle = R.string.app_manager;
                break;
            case ID_TOOLS_WIRELESS_FM:
                if (!onResume) mCurrentFragment = new WirelessFileManagerFragment();
                mTitle = mSubFragmentTitle = R.string.wireless_file_manager;
                break;
            //--------------------------------------------------------------------------------------
            case ID_PREFERENCES:
                if (!onResume) mCurrentFragment = new PreferencesFragment();
                mTitle = mFragmentTitle = R.string.preferences;
                mSubFragmentTitle = -1;
                break;
        }

        restoreActionBar();

        if (onResume) {
            return;
        }

        final boolean isSubFragment = mSubFragmentTitle != -1;

        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (!isSubFragment && fragmentManager.getBackStackEntryCount() > 0) {
            // set a lock to prevent calling setFragment as onResume gets called
            AppHelper.preventOnResume = true;
            MainActivity.sDisableFragmentAnimations = true;
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            MainActivity.sDisableFragmentAnimations = false;
            // release the lock
            AppHelper.preventOnResume = false;
        }

        final FragmentTransaction ft = fragmentManager.beginTransaction();

        if (isSubFragment) {
            ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right,
                    R.anim.slide_in_left, R.anim.slide_out_left);
            ft.addToBackStack(null);
        }

        ft.replace(R.id.container, mCurrentFragment);
        ft.commit();

        final MaterialMenuDrawable.IconState iconState;
        if (isSubFragment) {
            iconState = MaterialMenuDrawable.IconState.ARROW;
        } else {
            iconState = MaterialMenuDrawable.IconState.BURGER;
        }

        sMaterialMenu.animateState(iconState);
    }

    private void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
        }
    }

    public static void setSwipeOnContent(final boolean swipeOnContent) {
        if (sSlidingMenu == null) return;

        sSlidingMenu.setTouchModeAbove(swipeOnContent
                ? SlidingMenu.TOUCHMODE_FULLSCREEN : SlidingMenu.TOUCHMODE_MARGIN);
    }

}
