package dev.nolight.pingping;

public enum PreviewTarget {
	WHEN_SNEAKING, WHEN_NOT_SNEAKING, DISABLED;

	public boolean wants(boolean sneaking) {
		return switch (this) {
			case WHEN_SNEAKING -> sneaking;
			case WHEN_NOT_SNEAKING -> !sneaking;
			case DISABLED -> false;
		};
	}

	public String key() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
