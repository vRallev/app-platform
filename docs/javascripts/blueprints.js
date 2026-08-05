document.addEventListener("pointerdown", (event) => {
  if (!(event.target instanceof Element) || event.button !== 0) {
    return;
  }

  const handle = event.target.closest(".blueprint-demo__resize-handle");
  const demo = handle?.closest(".blueprint-demo");

  if (!demo) {
    return;
  }

  event.preventDefault();

  const pointerId = event.pointerId;
  const initialWidth = demo.getBoundingClientRect().width;
  const initialX = event.clientX;

  const resize = (moveEvent) => {
    if (moveEvent.pointerId === pointerId) {
      demo.style.width = `${Math.max(0, initialWidth + moveEvent.clientX - initialX)}px`;
    }
  };

  const stopResizing = (stopEvent) => {
    if (stopEvent.pointerId !== pointerId) {
      return;
    }

    demo.classList.remove("blueprint-demo--resizing");
    handle.removeEventListener("pointermove", resize);
    handle.removeEventListener("pointerup", stopResizing);
    handle.removeEventListener("pointercancel", stopResizing);
    handle.removeEventListener("lostpointercapture", stopResizing);

    if (handle.hasPointerCapture(pointerId)) {
      handle.releasePointerCapture(pointerId);
    }
  };

  handle.addEventListener("pointermove", resize);
  handle.addEventListener("pointerup", stopResizing);
  handle.addEventListener("pointercancel", stopResizing);
  handle.addEventListener("lostpointercapture", stopResizing);
  handle.setPointerCapture(pointerId);
  demo.classList.add("blueprint-demo--resizing");
});

document.addEventListener("keydown", (event) => {
  if (!(event.target instanceof Element)) {
    return;
  }

  const handle = event.target.closest(".blueprint-demo__resize-handle");
  const demo = handle?.closest(".blueprint-demo");
  const direction = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;

  if (!demo || direction === 0) {
    return;
  }

  event.preventDefault();
  demo.style.width = `${Math.max(0, demo.getBoundingClientRect().width + direction * 24)}px`;
});
