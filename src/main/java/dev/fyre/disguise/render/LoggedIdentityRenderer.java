package dev.fyre.disguise.render;

import java.util.Objects;
import java.util.logging.Logger;


public final class LoggedIdentityRenderer implements IdentityRenderer {
    private final Logger logger;
    public LoggedIdentityRenderer(Logger logger) { this.logger = Objects.requireNonNull(logger, "logger"); }
    @Override public void render(IdentityTransition transition) {
        logger.fine("Identity transition for viewer " + transition.getViewerId()
                + " subject " + transition.getSubjectId()
                + ": " + (transition.getAfter().isPresent() ? "disguise requested" : "restore requested"));
    }
}
