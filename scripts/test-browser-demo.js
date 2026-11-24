async page => {
  const base = /^https?:/.test(page.url()) ? new URL(page.url()).origin : "http://127.0.0.1:8083";
  const foto = "src/main/resources/demo-unggahan/ac-ruang.png";
  const bukti = "target/qa-screens";
  const assert = (ok, pesan) => { if (!ok) throw new Error(pesan); };
  const galat = [];
  page.on("pageerror", e => galat.push(e.message));
  const open = async (path, expected = 200) => {
    const response = await page.goto(base + path, { waitUntil: "domcontentloaded" });
    assert(response?.status() === expected, path + " HTTP " + response?.status());
  };
  const roles = {
    mahasiswa: "nadia@example.invalid",
    admin: "admin@example.invalid",
    petugas: "budi.santoso@example.invalid",
    pengganti: "dimas@example.invalid",
  };
  const sesiPeran = new Map();
  const masuk = async role => {
    await page.context().clearCookies();
    const ruang = "/" + (role === "pengganti" ? "petugas" : role);
    if (sesiPeran.has(role)) {
      await page.context().addCookies(sesiPeran.get(role));
      await open(ruang);
      assert(new URL(page.url()).pathname === ruang, "Sesi " + role + " tidak aktif");
      return;
    }
    await open("/login");
    await page.getByLabel("Email kampus atau NIM").fill(roles[role]);
    await page.getByRole("button", { name: "Masuk", exact: true }).click();
    await page.waitForURL(base + ruang);
    sesiPeran.set(role, await page.context().cookies(base));
  };
  const submit = async (form, path, pesan) => {
    const navigasi = page.waitForResponse(r => r.request().method() === "POST" && new URL(r.url()).pathname === path);
    await form.getByRole("button", { name: pesan, exact: true }).click();
    const result = await navigasi;
    assert(result.status() === 302 || result.status() === 303, path + " HTTP " + result.status());
    await page.waitForLoadState("domcontentloaded");
  };
  const notifikasi = async teks => {
    await page.locator(".lonceng").click();
    await page.locator("#pop-notifikasi").getByText(teks, { exact: false }).first().waitFor();
    await page.keyboard.press("Escape");
    await page.locator("#pop-notifikasi").waitFor({ state: "hidden" });
  };
  const buat = async (deskripsi, lampirkan) => {
    await open("/mahasiswa/laporan/baru");
    await page.locator("#fakultas").selectOption("Teknik");
    await page.locator("#gedung").selectOption("B11");
    await page.locator("#lantai").selectOption("2");
    await page.locator("#ruang").fill("204");
    await page.locator("#deskripsi").fill(deskripsi);
    if (lampirkan) {
      await page.locator("#foto").setInputFiles(foto);
      await page.locator("[data-nama-berkas]").waitFor();
      await page.getByRole("button", { name: "Hapus foto" }).click();
      assert(await page.locator("#foto").evaluate(el => el.files.length) === 0, "Foto gagal dihapus");
      await page.locator("#foto").setInputFiles(foto);
    }
    await page.getByRole("button", { name: "Kirim Laporan" }).click();
    await page.waitForURL(base + "/mahasiswa/laporan/*");
    const url = new URL(page.url()).pathname;
    const kode = (await page.locator(".kepala-hal p").first().innerText()).match(/UM-[0-9]+/)[0];
    if (!lampirkan) await page.getByText("Laporan ini dikirim tanpa foto bukti.").waitFor();
    return { url, kode };
  };
  const tugaskan = async kasus => {
    await open("/admin/kasus");
    await page.getByRole("link", { name: new RegExp(kasus.kode) }).first().click();
    const modal = page.getByRole("dialog");
    await modal.getByRole("heading", { name: "Detail laporan" }).waitFor();
    kasus.admin = new URL(await modal.locator("#form-tindak").getAttribute("action"), base).pathname.replace("/verifikasi", "");
    await modal.locator("#petugas-verifikasi").selectOption({ label: await modal.locator("#petugas-verifikasi option", { hasText: "Budi Santoso" }).textContent() });
    await modal.locator("#catatan-verifikasi").fill("Periksa kondisi dan tangani jika bisa.");
    await submit(modal, kasus.admin + "/verifikasi", "Verifikasi laporan");
    await page.getByText("Laporan terverifikasi dan petugas ditugaskan.", { exact: true }).waitFor();
  };
  const bukaTugas = async kasus => {
    await open("/petugas/tugas");
    const link = page.getByRole("link", { name: "Lihat tugas " + kasus.kode });
    kasus.tugas = await link.getAttribute("href");
    await link.click();
    await page.waitForURL(base + kasus.tugas);
  };
  const periksa = async (kasus, kritis) => {
    await page.getByRole("button", { name: "Simpan Pemeriksaan" }).click();
    assert(new URL(page.url()).pathname === kasus.tugas, "Form kosong dikirim");
    await page.getByRole("radio", { name: kritis ? "Ada bahaya kritis" : "Tidak ada", exact: true }).check();
    await page.locator("#gangguan").selectOption("RINGAN");
    await page.locator("#unitRusak").fill("1");
    await page.locator("#catatan").fill(kritis ? "Sambungan terbuka." : "Butuh baut baru.");
    await submit(page.locator("main"), kasus.tugas + "/pemeriksaan", "Simpan Pemeriksaan");
    await page.locator(".pill-prioritas").filter({ hasText: kritis ? "Kritis" : "Rendah" }).waitFor();
  };
  const selesai = async kasus => {
    await page.getByText("Selesaikan Perbaikan", { exact: true }).click();
    await page.locator("#catatan-perbaikan").fill("Komponen diganti dan diperiksa kembali.");
    await page.getByRole("button", { name: "Tandai Selesai", exact: true }).click();
    await page.locator("#foto-galat").filter({ hasText: /Lampirkan foto/ }).waitFor();
    await page.locator("#foto").setInputFiles(foto);
    await submit(page.locator("main"), kasus.tugas + "/selesai", "Tandai Selesai");
    await page.getByRole("heading", { name: "Hasil perbaikan" }).waitFor();
  };

  await page.setViewportSize({ width: 320, height: 740 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.context().clearCookies();
  await open("/");
  await page.getByRole("link", { name: "Laporkan kerusakan" }).waitFor();
  await masuk("mahasiswa");
  const tanda = Date.now();
  const langsung = await buat("Sambungan kabel terbuka " + tanda, true);
  const lanjut = await buat("Baut kursi perlu diganti " + tanda, false);
  await page.setViewportSize({ width: 1440, height: 900 });
  await masuk("admin");
  await tugaskan(langsung);
  await tugaskan(lanjut);

  await masuk("petugas");
  await notifikasi("Tugas baru " + lanjut.kode);
  await bukaTugas(langsung);
  await periksa(langsung, true);
  await submit(page.locator("main"), langsung.tugas + "/mulai", "Lanjutkan Perbaikan");
  await bukaTugas(lanjut);
  await periksa(lanjut, false);
  await page.getByText("Ajukan Tindak Lanjut", { exact: true }).click();
  await page.locator("#alasan").fill("Stok baut habis.");
  await page.locator("#kebutuhan").fill("Baut pengganti dan teknisi perabot.");
  await submit(page.locator("main"), lanjut.tugas + "/tindak-lanjut", "Kirim ke Admin");
  await page.getByRole("heading", { name: "Menunggu keputusan admin" }).waitFor();

  await masuk("mahasiswa");
  await open(lanjut.url);
  await page.getByText("Admin sedang meninjau langkah berikutnya.").waitFor();
  await notifikasi(lanjut.kode + " perlu tindak lanjut");

  await masuk("admin");
  await page.getByRole("link", { name: /kasus kritis aktif/i }).click();
  await page.getByRole("link", { name: new RegExp(langsung.kode) }).first().waitFor();
  await notifikasi("Tindak lanjut " + lanjut.kode);
  await open("/admin/kasus");
  await page.getByRole("link", { name: /^Perlu tindak lanjut/ }).click();
  await page.waitForURL(/tahap=TINDAK_LANJUT/);
  assert((await page.locator("#ekspor-laporan").getAttribute("href")).includes("tahap=TINDAK_LANJUT"), "Ekspor tidak mengikuti filter aktif");
  await page.getByRole("link", { name: new RegExp(lanjut.kode) }).first().click();
  const modal = page.getByRole("dialog");
  await modal.getByText("Stok baut habis.", { exact: true }).waitFor();
  await modal.getByText("Rendah", { exact: true }).waitFor();
  await modal.locator("#petugas-tindak-lanjut").selectOption({ label: await modal.locator("option", { hasText: "Dimas Pratama" }).textContent() });
  await modal.locator("#keputusan").fill("Dimas menerima baut dan melanjutkan pekerjaan.");
  await submit(modal, lanjut.admin + "/tindak-lanjut", "Simpan Keputusan");
  await page.getByText("Keputusan tersimpan.", { exact: false }).waitFor();

  await masuk("petugas");
  await open(lanjut.tugas, 404);
  await open(langsung.tugas);
  await selesai(langsung);
  await masuk("pengganti");
  await open(lanjut.tugas);
  await page.getByText("Dimas menerima baut dan melanjutkan pekerjaan.").waitFor();
  await page.getByText("Butuh baut baru.", { exact: true }).waitFor();
  await selesai(lanjut);

  await masuk("mahasiswa");
  await open(lanjut.url);
  await page.getByRole("link", { name: "Lihat bukti hasil perbaikan" }).waitFor();
  await page.context().clearCookies();
  await open("/login");
  await page.getByLabel("Email kampus atau NIM").fill("240535600202");
  await page.getByRole("button", { name: "Masuk", exact: true }).click();
  await page.waitForURL(base + "/mahasiswa");
  await open(lanjut.url, 404);

  const lebar = [320, 375, 390, 430, 768, 1024, 1366, 1440, 1920];
  const kelompok = [
    [null, ["/", "/login", "/akses-ditolak"]],
    ["mahasiswa", ["/mahasiswa", "/mahasiswa/laporan", "/mahasiswa/laporan/baru", lanjut.url, "/bantuan"]],
    ["petugas", ["/petugas", "/petugas/tugas", langsung.tugas]],
    ["admin", ["/admin", "/admin/kasus", "/admin/petugas", lanjut.admin]],
  ];
  const limpah = [];
  for (const [role, paths] of kelompok) {
    await page.setViewportSize({ width: 1440, height: 900 });
    if (role) await masuk(role); else await page.context().clearCookies();
    for (const path of paths) {
      await open(path, path === "/akses-ditolak" ? 403 : 200);
      for (const width of lebar) {
        await page.setViewportSize({ width, height: width <= 430 ? 740 : 900 });
        await page.evaluate(() => document.fonts.ready);
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
        if (overflow > 2) limpah.push([path, width, overflow]);
        if (width === 390 || width === 1440) {
          await page.screenshot({ path: bukti + "/" + (path === "/" ? "beranda" : path.slice(1).replaceAll("/", "-")) + "-" + width + ".png" });
        }
      }
    }
  }
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.setViewportSize({ width: 320, height: 700 });
  await open("/admin/kasus");
  await page.locator(".lonceng").focus();
  await page.keyboard.press("Enter");
  const panel = await page.locator("#pop-notifikasi").boundingBox();
  assert(panel?.x >= 0 && panel.x + panel.width <= 321, "Panel notifikasi keluar dari layar");
  await page.keyboard.press("Escape");
  assert(await page.locator(".lonceng").evaluate(el => document.activeElement === el), "Fokus tidak kembali");
  await open("/admin/kasus");
  await page.locator("a[data-modal]").first().click();
  const modalMobile = await page.getByRole("dialog").boundingBox();
  assert(modalMobile?.x >= 0 && modalMobile.x + modalMobile.width <= 321, "Dialog keluar dari layar");
  await page.keyboard.press("Escape");
  await page.getByRole("dialog").waitFor({ state: "hidden" });
  const browser = page.context().browser();
  if (browser) {
    const konteks = await browser.newContext({ javaScriptEnabled: false, viewport: { width: 390, height: 760 } });
    try {
      const tanpaJs = await konteks.newPage();
      await tanpaJs.goto(base + "/login");
      await tanpaJs.getByLabel("Email kampus atau NIM").fill(roles.admin);
      await tanpaJs.getByRole("button", { name: "Masuk", exact: true }).click();
      await tanpaJs.waitForURL(base + "/admin");
      await tanpaJs.goto(base + "/admin/kasus");
      await tanpaJs.locator("a[data-modal]").first().click();
      await tanpaJs.getByRole("heading", { name: "Detail laporan", level: 1 }).waitFor();
      assert(await tanpaJs.locator("#judul-modal").count() === 0, "Fragmen modal ikut tampil pada halaman detail");
    } finally {
      await konteks.close();
    }
  }
  assert(galat.length === 0, galat.join("\n"));
  assert(limpah.length === 0, "Overflow: " + JSON.stringify(limpah));
  return { hasil: "Dua jalur dan responsivitas lulus", kritis: langsung, rendah: lanjut, lebar };
}
