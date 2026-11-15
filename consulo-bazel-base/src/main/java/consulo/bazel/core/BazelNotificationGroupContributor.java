/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.core;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import java.util.function.Consumer;

/**
 * Replaces the upstream {@code <notificationGroup>} registrations of {@code intellij.bazel.core.xml}.
 * Upstream {@code NotificationGroupManager.getInstance().getNotificationGroup(id)} lookups use these constants.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class BazelNotificationGroupContributor implements NotificationGroupContributor {
    /**
     * id {@code Bazel}
     */
    public static final NotificationGroup BAZEL =
        new NotificationGroup("Bazel", BazelPluginLocalize.notificationgroupBazelTitle(), NotificationDisplayType.STICKY_BALLOON, true);
    /**
     * id {@code BazelPlugin}
     */
    public static final NotificationGroup BAZEL_PLUGIN =
        new NotificationGroup("BazelPlugin", BazelPluginLocalize.notificationgroupBazelpluginTitle(), NotificationDisplayType.BALLOON, true);
    /**
     * id {@code BazelSlowPlugins}
     */
    public static final NotificationGroup BAZEL_SLOW_PLUGINS =
        new NotificationGroup("BazelSlowPlugins", BazelPluginLocalize.notificationgroupBazelslowpluginsTitle(), NotificationDisplayType.BALLOON, true);

    @Override
    public void contribute(Consumer<NotificationGroup> registrator) {
        registrator.accept(BAZEL);
        registrator.accept(BAZEL_PLUGIN);
        registrator.accept(BAZEL_SLOW_PLUGINS);
    }
}
