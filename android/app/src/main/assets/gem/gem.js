/*
✨ CoonDev • https://dev.coonlink.com/

 ▄█▄    ████▄ ████▄    ▄   ██▄   ▄███▄      ▄
 █▀ ▀▄  █   █ █   █     █  █  █  █▀   ▀      █
 █   ▀  █   █ █   █ ██   █ █   █ ██▄▄   █     █
 █▄  ▄▀ ▀████ ▀████ █ █  █ █  █  █▄   ▄▀ █    █
 ▀███▀              █  █ █ ███▀  ▀███▀    █  █
                    █   ██                 █▐
                                           ▐
*/

(function (global) {
  function mountGem(canvas, opts) {
    opts = opts || {};
    var onReady = opts.onReady;
    var onError = opts.onError;
    var T = global.THREE;
    if (!T) { if (onError) onError(); return function () {}; }

    var r;
    try {r = new T.WebGLRenderer({ canvas: canvas, alpha: true, antialias: true, powerPreference: 'low-power' });} catch (e) {
      if (onError) onError();
      return function () {};
    }

    var dpr = Math.min(global.devicePixelRatio || 1, 2);
    var size = canvas.clientWidth || 160;
    r.setPixelRatio(dpr);
    r.setSize(size, size, false);
    r.setClearColor(0x000000, 0);
    var scene = new T.Scene();
    var cam = new T.PerspectiveCamera(30, 1, 0.1, 20);
    cam.position.z = 5;
    scene.add(new T.AmbientLight(0xffffff, 0.55));
    var key = new T.DirectionalLight(0xffffff, 1.35);
    key.position.set(2.4, 3.2, 4);
    scene.add(key);
    var fill = new T.DirectionalLight(0x9ad0ff, 0.55);
    fill.position.set(-2.5, -1.2, 2);
    scene.add(fill);
    var rim = new T.DirectionalLight(0xffffff, 0.7);
    rim.position.set(-1.5, 2.5, -3);
    scene.add(rim);
    var pts = [[0, -1],[0.72, 0.12],[0.48, 0.42],[0, 0.42]].map(function (p) { return new T.Vector2(p[0], p[1]); });
    var gem = new T.Mesh(new T.LatheGeometry(pts, 8), new T.MeshPhongMaterial({color: 0x1a8cff, specular: 0xffffff, shininess: 110, flatShading: true}));
    scene.add(gem);
    var auto = 0, dy = 0, dx = 0, ty = 0, tx = 0;
    var drag = false, moved = false, sx = 0, sy = 0, px = 0, py = 0;
    var raf = 0, last = 0, stopped = false;

    function frame(t) {
      if (stopped) return;
      raf = requestAnimationFrame(frame);
      var dt = Math.min(0.1, (t - last) / 1000 || 0.016);
      last = t;
      if (!drag) {
        auto += dt * Math.PI * 2 / 10.5;
        dy += (ty - dy) * 0.12;
        dx += (tx - dx) * 0.12;
      }
      gem.rotation.set(0.15 + dx, auto + dy, 0);
      r.render(scene, cam);
      if (onReady) {
        onReady();
        onReady = null;
      }
    }

    function down(e) {
      drag = true;
      moved = false;
      sx = px = e.offsetX;
      sy = py = e.offsetY;
      try { canvas.setPointerCapture(e.pointerId); } catch (err) {}
    }
    function move(e) {
      if (!drag) return;
      if (!moved && Math.hypot(e.offsetX - sx, e.offsetY - sy) < 8) return;
      moved = true;
      dy += (e.offsetX - px) * 0.01;
      dx += (e.offsetY - py) * 0.003;
      px = e.offsetX;
      py = e.offsetY;
    }
    function up() {
      if (!drag) return;
      drag = false;
      if (moved) { ty = tx = 0; return; }
      var h = canvas.clientWidth / 2;
      var q = (40 + Math.random() * 30) * Math.PI / 180;
      ty = (h - sx) / h * q;
      tx = (h - sy) / h * q * 0.5;
      setTimeout(function () { ty = tx = 0; }, 220);
    }

    canvas.addEventListener('pointerdown', down);
    canvas.addEventListener('pointermove', move);
    canvas.addEventListener('pointerup', up);
    canvas.addEventListener('pointercancel', up);
    raf = requestAnimationFrame(frame);

    return function stop() {
      stopped = true;
      cancelAnimationFrame(raf);
      canvas.removeEventListener('pointerdown', down);
      canvas.removeEventListener('pointermove', move);
      canvas.removeEventListener('pointerup', up);
      canvas.removeEventListener('pointercancel', up);
      try {
        gem.geometry.dispose();
        gem.material.dispose();
        r.dispose();
        r.forceContextLoss();
      } catch (err) {}
    };
  }

  global.mountGem = mountGem;
})(window);
