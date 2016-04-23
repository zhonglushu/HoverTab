package com.zhonglushu.example.hovertab.Utils;

/**
 * Created by zhonglushu on 2016/4/20.
 */
public enum Mode {

    /**
     * Disable all Pull-to-Refresh gesture and Refreshing handling
     */
    DISABLED(0x0),

    /**
     * Only allow the user to Pull from the start of the Refreshable View to
     * refresh. The start is either the Top or Left, depending on the
     * scrolling direction.
     */
    PULL_FROM_START(0x1),

    /**
     * Only allow the user to Pull from the end of the Refreshable View to
     * refresh. The start is either the Bottom or Right, depending on the
     * scrolling direction.
     */
    PULL_FROM_END(0x2),

    /**
     * Allow the user to both Pull from the start, from the end to refresh.
     */
    BOTH(0x3),

    /**
     * Disables Pull-to-Refresh gesture handling, but allows manually
     * setting the Refresh state via
     *
     */
    MANUAL_REFRESH_ONLY(0x4);

    /**
     * @deprecated Use {@link #PULL_FROM_START} from now on.
     */
    public static Mode PULL_DOWN_TO_REFRESH = Mode.PULL_FROM_START;

    /**
     * @deprecated Use {@link #PULL_FROM_END} from now on.
     */
    public static Mode PULL_UP_TO_REFRESH = Mode.PULL_FROM_END;

    /**
     * Maps an int to a specific mode. This is needed when saving state, or
     * inflating the view from XML where the mode is given through a attr
     * int.
     *
     * @param modeInt - int to map a Mode to
     * @return Mode that modeInt maps to, or PULL_FROM_START by default.
     */
    static Mode mapIntToValue(final int modeInt) {
        for (Mode value : Mode.values()) {
            if (modeInt == value.getIntValue()) {
                return value;
            }
        }

        // If not, return default
        return getDefault();
    }

    public static Mode getDefault() {
        return PULL_FROM_START;
    }

    private int mIntValue;

    // The modeInt values need to match those from attrs.xml
    Mode(int modeInt) {
        mIntValue = modeInt;
    }

    /**
     * @return true if the mode permits Pull-to-Refresh
     */
    boolean permitsPullToRefresh() {
        return !(this == DISABLED || this == MANUAL_REFRESH_ONLY);
    }

    /**
     * @return true if this mode wants the Loading Layout Header to be shown
     */
    public boolean showHeaderLoadingLayout() {
        return this == PULL_FROM_START || this == BOTH;
    }

    /**
     * @return true if this mode wants the Loading Layout Footer to be shown
     */
    public boolean showFooterLoadingLayout() {
        return this == PULL_FROM_END || this == BOTH || this == MANUAL_REFRESH_ONLY;
    }

    int getIntValue() {
        return mIntValue;
    }

}
