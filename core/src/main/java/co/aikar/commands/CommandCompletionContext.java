/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

public class CommandCompletionContext {
    private final RegisteredCommand command;
    protected final CommandIssuer issuer;
    private final String input;
    private final String config;
    private final Map<String, String> configs = Maps.newHashMap();
    private final List<String> args;

    CommandCompletionContext(RegisteredCommand command, CommandIssuer issuer, String input, String config, String[] args) {
        this.command = command;
        this.issuer = issuer;
        this.input = input;
        if (config != null) {
            String[] configs = ACFPatterns.COMMA.split(config);
            for (String conf : configs) {
                String[] confsplit = ACFPatterns.EQUALS.split(conf, 2);
                this.configs.put(confsplit[0].toLowerCase(), confsplit.length > 1 ? confsplit[1] : null);
            }
            this.config = configs[0];
        } else {
            this.config = null;
        }

        this.args = Lists.newArrayList(args);
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public String getConfig(String key) {
        return getConfig(key, null);
    }

    public String getConfig(String key, String def) {
        return this.configs.getOrDefault(key.toLowerCase(), def);
    }

    public boolean hasConfig(String key) {
        return this.configs.containsKey(key.toLowerCase());
    }

    public <T> T getContextValue(Class<? extends T> clazz) throws InvalidCommandArgument {
        return getContextValue(clazz, null);
    }

    public <T> T getContextValue(Class<? extends T> clazz, Integer paramIdx) throws InvalidCommandArgument {
        String name = null;
        if (paramIdx != null) {
            if (paramIdx >= command.parameters.length) {
                throw new IllegalArgumentException("Param index is higher than number of parameters");
            }
            Parameter param = command.parameters[paramIdx];
            Class<?> paramType = param.getType();
            if (!clazz.isAssignableFrom(paramType)) {
                throw new IllegalArgumentException(param.getName() +":" + paramType.getName() + " can not satisfy " + clazz.getName());
            }
            name = param.getName();
        } else {
            Parameter[] parameters = command.parameters;
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (clazz.isAssignableFrom(param.getType())) {
                    paramIdx = i;
                    name = param.getName();
                    break;
                }
            }
            if (paramIdx == null) {
                throw new IllegalStateException("Can not find any parameter that can satisfy " + clazz.getName());
            }
        }
        //noinspection unchecked
        Map<String, Object> resolved = command.resolveContexts(issuer, args, args.size());
        if (resolved == null || paramIdx > resolved.size()) {
            ACFLog.error("resolved: " + resolved + " paramIdx: " + paramIdx + " - size: " + (resolved != null ? resolved.size() : null ));
            ACFUtil.sneaky(new CommandCompletionTextLookupException());
        }

        //noinspection unchecked
        return (T) resolved.get(name);
    }

    public CommandIssuer getIssuer() {
        return issuer;
    }

    public String getInput() {
        return input;
    }

    public String getConfig() {
        return config;
    }
}