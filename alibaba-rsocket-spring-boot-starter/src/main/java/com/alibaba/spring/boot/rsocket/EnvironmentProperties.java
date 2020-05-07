package com.alibaba.spring.boot.rsocket;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.*;

/**
 * environment with properties wrapper
 *
 * @author linux_china
 */
public class EnvironmentProperties extends Properties {
    private Environment env;

    public EnvironmentProperties(Environment env) {
        this.env = env;
    }

    @Override
    public String getProperty(String key) {
        return env.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    @Override
    public Enumeration<?> propertyNames() {
        Set<String> names = new HashSet<>();
        for (PropertySource<?> propertySource : ((AbstractEnvironment) env).getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                Collections.addAll(names, ((EnumerablePropertySource) propertySource).getPropertyNames());
            }
        }
        return Collections.enumeration(names);
    }

    @Override
    public Set<String> stringPropertyNames() {
        Set<String> names = new HashSet<>();
        for (Enumeration<?> e = propertyNames(); e.hasMoreElements(); ) {
            Object k = e.nextElement();
            if (k instanceof String) {
                names.add((String) k);
            }
        }
        return names;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EnvironmentProperties that = (EnvironmentProperties) o;

        return Objects.equals(env, that.env);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (env != null ? env.hashCode() : 0);
        return result;
    }
}
