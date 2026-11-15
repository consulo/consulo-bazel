package org.jetbrains.bazel.workspace.fileEvents;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.event.BulkFileListenerBackgroundable;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;

import java.util.ArrayList;
import java.util.List;

@TopicImpl(ComponentScope.APPLICATION)
public class BazelFileEventListener implements BulkFileListenerBackgroundable {
    @Override
    public void after(List<? extends VFileEvent> events) {
        List<Project> projects = new ArrayList<>();
        // ProjectLocator::getProjectsForFile won't work, since it only recognizes files already added to content roots
        for (Project it : ProjectManager.getInstance().getOpenProjects()) {
            if (org.jetbrains.bazel.config.project.isBazelProject(it)) {
                projects.add(it);
            }
        }
        List<VFileEvent> eventList = List.copyOf(events);
        for (Project project : projects) {
            BazelCoroutineService.getInstance(project).start(() ->
                BazelFileEventProcessor.getInstance(project).enqueue(eventList)
            );
        }
    }
}
