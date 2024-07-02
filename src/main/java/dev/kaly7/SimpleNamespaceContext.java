package dev.kaly7;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext{
        private final Map<String, String> namespaces;

    public SimpleNamespaceContext(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return namespaces.get(prefix);
    }

    @Override
    public String getPrefix(String uri) {
        return namespaces.entrySet().stream()
                .filter(entry -> entry.getValue().equals(uri))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    @Override
    public Iterator<String> getPrefixes(String uri) {
        return namespaces.keySet().iterator();
    }
    
}
