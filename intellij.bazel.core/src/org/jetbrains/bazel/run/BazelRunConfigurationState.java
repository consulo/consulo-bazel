package org.jetbrains.bazel.run;

import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.SkipDefaultsSerializationFilter;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

import java.util.Collection;

// consulo: upstream is a BaseState (xmlb property delegates); subclasses are plain beans serialized by XmlSerializer
public abstract class BazelRunConfigurationState<T extends BazelRunConfigurationState<T>> {
    /**
     * Loads this handler's state from the external data.
     */
    public void readExternal(Element element) throws InvalidDataException {
        XmlSerializer.deserializeInto(this, element);
    }

    /**
     * Updates the element with the handler's state.
     */
    public void writeExternal(Element element) throws WriteExternalException {
        // consulo: like upstream serializeObjectInto() of a BaseState, default values are not written
        XmlSerializer.serializeInto(this, element, new SkipDefaultsSerializationFilter());
    }

    // consulo: upstream implements FragmentedSettings and stores the fragments shown by "Modify options" here; the Consulo
    // fragment editor bridge shows every fragment, so there are no selected options
    // @get:XCollection(propertyElementName = "selectedOptions")
    // override var selectedOptions: MutableList<FragmentedSettings.Option> by list()

    public abstract Collection<SettingsEditorFragment<BazelRunConfiguration, ?>> createFragments(BazelRunConfiguration configuration);
}
