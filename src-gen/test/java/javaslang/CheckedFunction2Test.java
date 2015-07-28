/*     / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckedFunction2Test {

    @Test
    public void shouldLift() {
        class Type {
            Object methodReference(Object o1, Object o2) {
                return null;
            }
        }
        final Type type = new Type();
        assertThat(CheckedFunction2.lift(type::methodReference)).isNotNull();
    }

    @Test
    public void shouldPartiallyApplyWith1Arguments() throws Throwable {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        assertThat(f.apply(null)).isNotNull();
    }

    @Test
    public void shouldRecognizeApplicabilityOfNull() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        assertThat(f.isApplicableTo(null, null)).isTrue();
    }

    @Test
    public void shouldRecognizeApplicabilityOfNonNull() {
        final CheckedFunction2<Integer, Integer, Integer> f = (i1, i2) -> null;
        assertThat(f.isApplicableTo(1, 2)).isTrue();
    }

    @Test
    public void shouldRecognizeApplicabilityToTypes() {
        final CheckedFunction2<Integer, Integer, Integer> f = (i1, i2) -> null;
        assertThat(f.isApplicableToTypes(Integer.class, Integer.class)).isTrue();
    }

    @Test
    public void shouldGetArity() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        assertThat(f.arity()).isEqualTo(2);
    }

    @Test
    public void shouldCurry() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final CheckedFunction1<Object, CheckedFunction1<Object, Object>> curried = f.curried();
        assertThat(curried).isNotNull();
    }

    @Test
    public void shouldTuple() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final CheckedFunction1<Tuple2<Object, Object>, Object> tupled = f.tupled();
        assertThat(tupled).isNotNull();
    }

    @Test
    public void shouldReverse() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        assertThat(f.reversed()).isNotNull();
    }

    @Test
    public void shouldMemoize() throws Throwable {
        final AtomicInteger integer = new AtomicInteger();
        final CheckedFunction2<Integer, Integer, Integer> f = (i1, i2) -> i1 + i2 + integer.getAndIncrement();
        final CheckedFunction2<Integer, Integer, Integer> memo = f.memoized();
        final int expected = memo.apply(1, 2);
        assertThat(memo.apply(1, 2)).isEqualTo(expected);
    }

    @Test
    public void shouldComposeWithAndThen() {
        final CheckedFunction2<Object, Object, Object> f = (o1, o2) -> null;
        final CheckedFunction1<Object, Object> after = o -> null;
        final CheckedFunction2<Object, Object, Object> composed = f.andThen(after);
        assertThat(composed).isNotNull();
    }

}