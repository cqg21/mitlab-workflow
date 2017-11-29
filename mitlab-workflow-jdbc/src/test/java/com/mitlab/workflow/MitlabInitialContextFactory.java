package com.mitlab.workflow;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class MitlabInitialContextFactory implements InitialContextFactory {
    private static final Context CONTEXT;

    static {
        CONTEXT = new Context() {
            private Map<String, Object> cache = new HashMap<String, Object>();

            @Override
            public Object lookup(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Object lookup(String name) throws NamingException {
                return cache.get(name);
            }

            @Override
            public void bind(Name name, Object obj) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void bind(String name, Object obj) throws NamingException {
                cache.put(name, obj);
            }

            @Override
            public void rebind(Name name, Object obj) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void rebind(String name, Object obj) throws NamingException {
                cache.put(name, obj);
            }

            @Override
            public void unbind(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void unbind(String name) throws NamingException {
                cache.remove(name);
            }

            @Override
            public void rename(Name oldName, Name newName) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void rename(String oldName, String newName) throws NamingException {
                cache.put(newName, cache.remove(oldName));
            }

            @Override
            public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void destroySubcontext(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void destroySubcontext(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Context createSubcontext(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Context createSubcontext(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Object lookupLink(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Object lookupLink(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public NameParser getNameParser(Name name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public NameParser getNameParser(String name) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Name composeName(Name name, Name prefix) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public String composeName(String name, String prefix) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Object addToEnvironment(String propName, Object propVal) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Object removeFromEnvironment(String propName) throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public Hashtable<?, ?> getEnvironment() throws NamingException {
                throw new UnsupportedOperationException("");
            }

            @Override
            public void close() throws NamingException {

            }

            @Override
            public String getNameInNamespace() throws NamingException {
                throw new UnsupportedOperationException("");
            }
        };
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return CONTEXT;
    }
}
