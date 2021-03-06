/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package javaslang.match;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

// ------------
// TODO: check generic (return) type parameters (-> e.g. wildcard is not allowed)
// TODO: detect collisions / retry with different generic type arg names (starting with _1, _2, ...)
// ------------
class Generator {

    // ENTRY POINT: Expands one @Patterns class
    static Optional<String> generate(TypeElement typeElement, Messager messager) {
        List<ExecutableElement> executableElements = getMethods(typeElement, messager);
        if (executableElements.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.WARNING, "No @Unapply methods found.", typeElement);
            return Optional.empty();
        } else {
            final String _package = Elements.getPackage(typeElement);
            final String _class = Elements.getSimpleName(typeElement);
            final ImportManager im = ImportManager.of(_package);
            im.getStatic("javaslang.API.Match.*");
            final String methods = generate(im, typeElement, executableElements);
            final String result = (_package.isEmpty() ? "" : "package " + _package + ";\n\n") +
                    im.getImports() +
                    "\n\n// GENERATED BY JAVASLANG <<>> derived from " + typeElement.getQualifiedName() + "\n\n" +
                    "public final class " + _class + " {\n\n" +
                    "    private " + _class + "() {\n" +
                    "    }\n\n" +
                    methods +
                    "}\n";
            return Optional.of(result);
        }
    }

    // Expands the @Unapply methods of a @Patterns class
    private static String generate(ImportManager im, TypeElement typeElement, List<ExecutableElement> executableElements) {
        final StringBuilder builder = new StringBuilder();
        for (ExecutableElement executableElement : executableElements) {
            generate(im, typeElement, executableElement, builder);
            builder.append("\n");
        }
        return builder.toString();
    }

    // Expands one @Unapply method
    private static void generate(ImportManager im, TypeElement type, ExecutableElement elem, StringBuilder builder) {
        final String typeName = im.getType(Elements.getRawParameterType(elem, 0));
        final String name = elem.getSimpleName().toString();
        final int arity = getArity(elem);
        final String args = getArgs(elem, arity);
        final String unapplyRef = type.getSimpleName() + "::" + name;
        final String body;
        if (arity == 0) {
            body = "Pattern0.of(" + typeName + ".class)";
        } else {
            body = String.format("Pattern%d.of(%s, %s, %s)", arity, typeName + ".class", args, unapplyRef);
        }
        final String generics = getGenerics(im, elem);
        final String returnType = getReturnType(im, elem, arity);
        final String params = getParams(im, elem, arity);
        final String method;
        if (Elements.hasTypeParameters(elem)) {
            method = String.format("%s %s %s(%s) {\n        return %s;\n    }", generics, returnType, name, params, body);
        } else {
            method = String.format("final %s %s = %s;", returnType, name, body);
        }
        builder.append("    public static ").append(method).append("\n");
    }

    // Expands the generics part of a method declaration
    private static String getGenerics(ImportManager im, ExecutableElement elem) {
        final String[] typeParameters = Elements.getTypeParameters(elem);
        final String[] returnTypeArgs = Elements.getReturnTypeArgs(elem);
        if (typeParameters.length + returnTypeArgs.length == 0) {
            return "";
        } else {
            final List<String> result = new ArrayList<>();
            result.addAll(Arrays.asList(typeParameters));
            for (int i = 0; i < returnTypeArgs.length; i++) {
                final String returnTypeArg = importedTypeArg(im, returnTypeArgs[i]);
                result.add("_" + (i + 1) + " extends " + returnTypeArg);
            }
            return result.stream().collect(joining(", ", "<", ">"));
        }
    }

    // Expands the return type of a method declaration
    private static String getReturnType(ImportManager im, ExecutableElement elem, int arity) {
        final String type = importedTypeArg(im, Elements.getParameterType(elem, 0));
        if (arity == 0) {
            return "Pattern0<" + type + ">";
        } else {
            final List<String> resultTypes = new ArrayList<>();
            final int typeParameterCount = Elements.getReturnTypeArgs(elem).length;
            resultTypes.add(type);
            for (int i = 0; i < typeParameterCount; i++) {
                resultTypes.add("_" + (i + 1));
            }
            return "Pattern" + arity + "<" + resultTypes.stream().collect(joining(", ")) + ">";
        }
    }

    private static String getArgs(ExecutableElement elem, int arity) {
        return IntStream.rangeClosed(1, arity).mapToObj(i -> "p" + i).collect(joining(", "));
    }

    private static String getParams(ImportManager im, ExecutableElement elem, int arity) {
        final String patternType = im.getType("javaslang.API.Match.Pattern");
        return IntStream.rangeClosed(1, arity).mapToObj(i -> patternType + "<_" + i + ", ?> p" + i).collect(joining(", "));
    }

    // returns all @Unapply methods of a @Patterns class
    private static List<ExecutableElement> getMethods(TypeElement typeElement, Messager messager) {
        if (Patterns.Checker.isValid(typeElement, messager)) {
            return typeElement.getEnclosedElements().stream()
                    .filter(element -> element.getAnnotationsByType(Unapply.class).length == 1 &&
                            element instanceof ExecutableElement &&
                            Unapply.Checker.isValid((ExecutableElement) element, messager))
                    .map(element -> (ExecutableElement) element)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    // Not part of Elements helper because specific for this use-case (return type Tuple)
    private static int getArity(ExecutableElement elem) {
        final DeclaredType returnType = (DeclaredType) elem.getReturnType();
        final String simpleName = returnType.asElement().getSimpleName().toString();
        return Integer.parseInt(simpleName.substring("Tuple".length()));
    }

    // imports type args (currently not recursively)
    private static String importedTypeArg(ImportManager im, String typeArg) {
        return typeArg.contains(".") ? im.getType(typeArg) : typeArg;
    }

    /**
     * A <em>stateful</em> ImportManager which generates an import section of a Java class file.
     */
    private static class ImportManager {

        static final int DEFAULT_WILDCARD_THRESHOLD = 5;

        // properties
        private final String packageNameOfClass;
        private final Set<String> knownSimpleClassNames;
        private final int wildcardThreshold;

        // mutable state
        private Map<String, String> nonStaticImports = new HashMap<>();
        private Map<String, String> staticImports = new HashMap<>();

        public ImportManager(String packageNameOfClass, Set<String> knownSimpleClassNames, int wildcardThreshold) {
            this.packageNameOfClass = packageNameOfClass;
            this.knownSimpleClassNames = knownSimpleClassNames;
            this.wildcardThreshold = wildcardThreshold;
        }

        public static ImportManager of(String packageNameOfClass) {
            return new ImportManager(packageNameOfClass, Collections.emptySet(), DEFAULT_WILDCARD_THRESHOLD);
        }

        // used by generator to register non-static imports
        public String getType(String fullQualifiedName) {
            final String[] split = splitGenerics(fullQualifiedName);
            return simplify(split[0], nonStaticImports, packageNameOfClass, knownSimpleClassNames) + split[1];
        }

        // used by generator to register static imports
        public String getStatic(String fullQualifiedName) {
            final String[] split = splitGenerics(fullQualifiedName);
            return simplify(split[0], staticImports, packageNameOfClass, knownSimpleClassNames) + split[1];
        }

        // finally used by generator to get the import section
        public String getImports() {
            final String staticImportSection = optimizeImports(staticImports.keySet(), true, wildcardThreshold);
            final String nonStaticImportSection = optimizeImports(nonStaticImports.keySet(), false, wildcardThreshold);
            return staticImportSection + "\n\n" + nonStaticImportSection;
        }

        private static String optimizeImports(Set<String> imports, boolean isStatic, int wildcardThreshold) {
            final Map<String, Integer> counts = imports.stream()
                    .map(ImportManager::getPackageName)
                    .collect(groupingBy(s -> s))
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));

            final List<String> directImports = imports.stream()
                    .filter(s -> counts.get(getPackageName(s)) <= wildcardThreshold)
                    .collect(toList());

            final List<String> wildcardImports = counts.entrySet().stream()
                    .filter(entry -> entry.getValue() > wildcardThreshold)
                    .map(entry -> entry.getKey() + ".*")
                    .collect(toList());

            final List<String> result = new ArrayList<>(directImports);
            result.addAll(wildcardImports);

            final String prefix = "import " + (isStatic ? "static " : "");
            return result.stream().sorted().map(s -> prefix + s + ";").collect(joining("\n"));
        }

        private static String simplify(String fullQualifiedName, Map<String, String> imports, String packageNameOfClass, Set<String> knownSimpleClassNames) {
            final String simpleName = getSimpleName(fullQualifiedName);
            final String packageName = getPackageName(fullQualifiedName);
            if (packageName.isEmpty() && !packageNameOfClass.isEmpty()) {
                throw new IllegalStateException("Can't import class '" + simpleName + "' located in default package");
            } else if (packageName.equals(packageNameOfClass)) {
                return simpleName;
            } else if (imports.containsKey(fullQualifiedName)) {
                return imports.get(fullQualifiedName);
            } else if (knownSimpleClassNames.contains(simpleName) || imports.values().contains(simpleName)) {
                return fullQualifiedName;
            } else {
                imports.put(fullQualifiedName, simpleName);
                return simpleName;
            }
        }

        private static String getPackageName(String fqn) {
            return fqn.substring(0, Math.max(fqn.lastIndexOf('.'), 0));
        }

        private static String getSimpleName(String fqn) {
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }

        private static String[] splitGenerics(String fqn) {
            final int i = fqn.indexOf("<");
            return (i == -1) ? new String[] { fqn, "" } : new String[] { fqn.substring(0, i), fqn.substring(i) };
        }
    }
}
