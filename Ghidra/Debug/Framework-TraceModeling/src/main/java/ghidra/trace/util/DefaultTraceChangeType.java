/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.trace.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import ghidra.framework.model.DomainObjectChangeRecord;

/**
 * A trace change type, assigned a number at runtime
 *
 * @param <T> the type of object changed
 * @param <U> the type of the object's attribute that changed
 */
public class DefaultTraceChangeType<T, U> implements TraceChangeType<T, U> {

	private static final Map<TraceEventType, String> TYPE_NAMES = new HashMap<>();
	private static final Set<Field> FIELD_BACKLOG = new HashSet<>();

	private static void procType(Field f, TraceChangeType<?, ?> type) {
		String kind = f.getDeclaringClass().getSimpleName();
		if (kind.startsWith("Trace")) {
			kind = kind.substring("Trace".length());
		}
		if (kind.endsWith("ChangeType")) {
			kind = kind.substring(0, kind.length() - "ChangeType".length());
		}
		TYPE_NAMES.put(type.getEventType(), kind + "." + f.getName());
	}

	private static <C extends TraceChangeType<?, ?>> void procField(Field f, Class<C> cls,
			boolean isBacklog) {
		int mods = f.getModifiers();
		if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods)) {
			return;
		}
		if (!cls.isAssignableFrom(f.getType())) {
			return;
		}
		C type;
		try {
			type = cls.cast(f.get(null));
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
		if (type != null) {
			procType(f, type);
		}
		else if (isBacklog) {
			throw new AssertionError();
		}
		else {
			FIELD_BACKLOG.add(f);
		}
	}

	@SuppressWarnings("unchecked")
	private static void procBacklog() {
		for (Iterator<Field> fit = FIELD_BACKLOG.iterator(); fit.hasNext();) {
			Field f = fit.next();
			procField(f, (Class<? extends TraceChangeType<?, ?>>) f.getType(), true);
			fit.remove();
		}
	}

	private static <C extends TraceChangeType<?, ?>> void scanTypeNames(Class<C> cls) {
		for (Field f : cls.getFields()) {
			procField(f, cls, false);
		}
	}

	public static String getName(int type) {
		procBacklog();
		String name = TYPE_NAMES.get(type);
		if (name != null) {
			return name;
		}
		return "TYPE_0x" + Integer.toHexString(type);
	}

	private final TraceEventType eventType;

	public DefaultTraceChangeType() {
		this.eventType = new TraceEventType(getClass().getSimpleName());

		scanTypeNames(getClass());
	}

	@Override
	public TraceEventType getEventType() {
		return eventType;
	}

	@SuppressWarnings("unchecked")
	public TraceChangeRecord<T, U> cast(DomainObjectChangeRecord rec) {
		return (TraceChangeRecord<T, U>) rec;
	}
}
