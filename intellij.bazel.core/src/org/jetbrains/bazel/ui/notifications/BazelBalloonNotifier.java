package org.jetbrains.bazel.ui.notifications;

import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Facilitates displaying balloon notifications. Notifications will display as follows:
 * <pre>
 * --------------------------
 * | [icon] &lt;build tool id&gt;: [subtitle] |
 * |        [content]       |
 * --------------------------
 * </pre>
 * Custom icons are used only in {@code info(...)}; Bazel icon is used by default.
 * <p>
 * In {@code warn(...)} and {@code error(...)} default icons are used, because
 * the icon is the only difference between error, warning and information.
 * <p>
 * If subtitle is empty, there will be no {@code ':'} character after "Bazel".
 */
public final class BazelBalloonNotifier {
    private BazelBalloonNotifier() {
    }

    /**
     * Display an information as a balloon notification
     *
     * @param title      title of the notification
     * @param content    main content of the notification
     * @param subtitle   subtitle of the notification (none by default)
     * @param customIcon icon for the notification
     */
    public static void info(
        LocalizeValue title,
        LocalizeValue content,
        LocalizeValue subtitle,
        Image customIcon
    ) {
        notify(title, content, subtitle, NotificationType.INFORMATION, customIcon);
    }

    // consulo: overload for the upstream default argument subtitle = ""
    public static void info(
        LocalizeValue title,
        LocalizeValue content,
        Image customIcon
    ) {
        info(title, content, LocalizeValue.empty(), customIcon);
    }

    /**
     * Display a warning as a balloon notification
     *
     * @param title    title of the notification
     * @param content  main content of the notification
     * @param subtitle subtitle of the notification (none by default)
     */
    public static void warn(
        LocalizeValue title,
        LocalizeValue content,
        LocalizeValue subtitle
    ) {
        notify(title, content, subtitle, NotificationType.WARNING, null);
    }

    // consulo: overload for the upstream default argument subtitle = ""
    public static void warn(
        LocalizeValue title,
        LocalizeValue content
    ) {
        warn(title, content, LocalizeValue.empty());
    }

    /**
     * Display an error as a balloon notification
     *
     * @param title    title of the notification
     * @param content  main content of the notification
     * @param subtitle subtitle of the notification (none by default)
     */
    public static void error(
        LocalizeValue title,
        LocalizeValue content,
        LocalizeValue subtitle
    ) {
        notify(title, content, subtitle, NotificationType.ERROR, null);
    }

    // consulo: overload for the upstream default argument subtitle = ""
    public static void error(
        LocalizeValue title,
        LocalizeValue content
    ) {
        error(title, content, LocalizeValue.empty());
    }

    private static void notify(
        LocalizeValue title,
        LocalizeValue content,
        LocalizeValue subtitle,
        NotificationType notificationType,
        @Nullable Image customIcon
    ) {
        // consulo: BALLOON_ID ("BazelPlugin") is the group BazelNotificationGroupContributor.BAZEL_PLUGIN
        NotificationService.getInstance()
            .newOfType(BazelNotificationGroupContributor.BAZEL_PLUGIN, notificationType)
            .content(content.map(it -> "<html>" + it + "</html>"))
            .title(title)
            .subtitle(subtitle)
            .optionalIcon(customIcon)
            .notify(null);
    }
}
