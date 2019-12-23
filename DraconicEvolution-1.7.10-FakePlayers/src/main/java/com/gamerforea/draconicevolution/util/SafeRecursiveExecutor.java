package com.gamerforea.draconicevolution.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;

public final class SafeRecursiveExecutor
{
	public static final SafeRecursiveExecutor INSTANCE = new SafeRecursiveExecutor();
	private final ThreadLocal<Set<? super Object>> callStack = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

	@Nullable
	public <T, R> R execute(@Nullable T object, @Nonnull Function<T, R> function)
	{
		return this.execute(object, function, false);
	}

	@Nullable
	public <T, R> R execute(@Nullable T object, @Nonnull Function<T, R> function, boolean forceNullExecute)
	{
		if (forceNullExecute && object == null)
			return function.apply(null);

		Set<? super Object> callStack = this.callStack.get();
		if (callStack.add(object))
			try
			{
				return function.apply(object);
			}
			finally
			{
				callStack.remove(object);
			}

		return null;
	}
}
