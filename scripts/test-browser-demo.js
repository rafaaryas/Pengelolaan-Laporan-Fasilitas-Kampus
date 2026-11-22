async page => {
  // Alur penuh lewat browser: landing → login per peran → aksi inti → keluar.
  // Jalankan terhadap skema QA yang baru dimigrasi (lihat docs/TEST_RESULTS.md).
  const base = "http://127.0.0.1:8081";
  const assert = (condition, message) => {
    if (!condition) throw new Error(message);
  };
  const open = async path => {
    const response = await page.goto(base + path);
    assert(response?.status() === 200, path + " mengembalikan " + response?.status());
    assert(page.url() === base + path, "Pengalihan tak terduga dari " + path + " ke " + page.url());
  };
  const visible = async (text, selector = "main") => {
    await page.locator(selector).getByText(text, { exact: false }).first().waitFor({ state: "visible" });
  };
  const click = async name => page.getByRole("button", { name, exact: true }).click();
  const keluar = async () => {
    await click("Keluar");
    await page.waitForURL(base + "/?keluar");
    await visible("Anda sudah keluar");
  };
  const masuk = async (identitas, ruang) => {
    if (await page.getByRole("button", { name: "Keluar", exact: true }).count()) await keluar();
    await open("/login");
    await page.getByLabel("Email kampus atau NIM").fill(identitas);
    await click("Masuk");
    await page.waitForURL(base + ruang);
  };
  const laporan = async (fasilitas, jenis, uraian) => {
    await open("/mahasiswa/laporan/baru");
    const option = page.locator("#fasilitasId option").filter({ hasText: new RegExp("^" + fasilitas + " ·") });
    await page.locator("#fasilitasId").selectOption(await option.getAttribute("value"));
    assert((await page.locator("#lokasi").innerText()).startsWith("Gedung"), "Lokasi fasilitas tidak diperbarui");
    await page.locator(`input[name="jenisKerusakan"][value="${jenis}"]`).check();
    await page.getByLabel("Deskripsi kerusakan").fill(uraian);
    await click("Kirim laporan");
    await visible("Laporan berhasil dikirim.");
    await visible("Menunggu verifikasi", ".m-detail-status");
    assert(await page.locator(".m-detail-status .prio").count() === 0, "Prioritas muncul sebelum verifikasi");
    return {
      laporan: new URL(page.url()).pathname,
      kasus: (await page.locator(".fakta .kode").first().innerText()).slice(2)
    };
  };
  const periksa = async (kasus, bahaya, gangguan, unit, catatan) => {
    await open("/petugas/kasus/" + kasus + "/verifikasi");
    await page.locator(`input[name="bahayaKritis"][value="${bahaya}"]`).check();
    await page.locator(`input[name="gangguan"][value="${gangguan}"]`).check();
    await page.locator("#unitRusak").fill(String(unit));
    await page.locator("#catatan").fill(catatan);
  };
  const tugaskan = async (kasus, nama) => {
    await open("/admin/kasus/" + kasus);
    await page.locator("#petugasId").selectOption({ label: nama });
    await page.getByLabel("Pesan untuk petugas").fill("Periksa dan pulihkan fungsi fasilitas.");
    await click("Tugaskan petugas");
    await visible("Petugas berhasil ditugaskan.");
    await visible(nama, ".a-detail-samping");
  };

  await page.setViewportSize({ width: 1440, height: 1000 });
  await open("/");
  await page.getByRole("link", { name: "Masuk dan laporkan kerusakan" }).click();
  await page.waitForURL(base + "/login");
  await page.screenshot({ path: ".cache/browser/login-desktop.png" });
  await click("Masuk");
  await visible("Isi email kampus atau NIM");
  await page.getByLabel("Email kampus atau NIM").fill("bukan-email");
  await click("Masuk");
  await visible("Format belum sesuai");
  await page.screenshot({ path: ".cache/browser/login-galat.png" });

  const ditolak = await page.goto(base + "/admin");
  assert(page.url() === base + "/login", "Ruang admin terbuka tanpa masuk.");
  await masuk("andi@example.invalid", "/mahasiswa");
  const larangan = await page.goto(base + "/admin");
  assert(larangan.status() === 403, "Mahasiswa dapat membuka ruang admin.");
  await visible("bukan untuk ruang kerja Anda");
  await open("/mahasiswa");
  await page.screenshot({ path: ".cache/browser/mahasiswa-desktop.png" });
  await open("/mahasiswa/laporan/baru");
  await page.locator("form.m-form").evaluate(form => { form.noValidate = true; });
  await click("Kirim laporan");
  await visible("Isi deskripsi kerusakan.");

  const pertama = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 1 tidak dingin.");
  await masuk("240535600102", "/mahasiswa");
  const budi = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 1 masih tidak dingin.");
  assert(pertama.kasus === budi.kasus, "Laporan Budi belum tergabung.");
  await masuk("andi@example.invalid", "/mahasiswa");
  const andi = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 2 mati total.");
  assert(pertama.kasus === andi.kasus, "Laporan berulang seharusnya tergabung.");
  const berbeda = await laporan("AC B13", "KEBOCORAN", "QA: Pipa AC bocor.");
  assert(berbeda.kasus !== pertama.kasus, "Jenis kerusakan berbeda masuk kasus yang sama.");

  await masuk("admin@example.invalid", "/admin");
  await open("/admin/kasus/" + pertama.kasus);
  await visible("3 laporan · 2 pelapor");
  assert(await page.getByRole("button", { name: "Tugaskan petugas" }).count() === 0, "Kasus belum diperiksa dapat ditugaskan.");
  page.once("dialog", dialog => dialog.accept());
  await page.locator(".aduan").filter({ hasText: "QA: AC 2 mati total." }).getByRole("button", { name: "Pisahkan ke kasus baru" }).click();
  await visible("Laporan dipindahkan ke kasus baru");
  const hasilPisah = new URL(page.url()).pathname.split("/").at(-1);
  assert(hasilPisah !== pertama.kasus, "Kasus koreksi sama dengan kasus asal.");

  await masuk("sinta@example.invalid", "/mahasiswa");
  const kabel = await laporan("Instalasi listrik", "BAHAYA_LISTRIK", "QA: Kabel terbuka di lantai 2.");
  const lampu = await laporan("Lampu ruang kelas", "TIDAK_BERFUNGSI", "QA: Satu lampu tidak menyala.");

  // Akun mahasiswa baru dibuat otomatis untuk email yang belum terdaftar
  await masuk("mahasiswa.baru@students.um.ac.id", "/mahasiswa");
  await visible("Belum ada laporan");
  const kursi = await laporan("Kursi kuliah", "FISIK", "QA: Sandaran kursi patah.");
  const bukanMilik = await page.goto(base + andi.laporan);
  assert(bukanMilik.status() === 404, "Mahasiswa lain dapat membuka laporan Andi.");

  await masuk("dimas@example.invalid", "/petugas");
  await page.screenshot({ path: ".cache/browser/petugas-desktop.png" });
  await periksa(pertama.kasus, false, "BERAT", 10, "QA: Kompresor perlu diganti.");
  await page.locator("form.p-form").evaluate(form => { form.noValidate = true; });
  await click("Simpan hasil pemeriksaan");
  await visible("jumlah unit rusak antara 1 dan 4", ".alert");
  await page.locator("#unitRusak").fill("1");
  await click("Simpan hasil pemeriksaan");
  await visible("Verifikasi disimpan.");
  await masuk("raka@example.invalid", "/petugas");
  await periksa(kabel.kasus, true, "RINGAN", 1, "QA: Konduktor terbuka.");
  await click("Simpan hasil pemeriksaan");
  await visible("Verifikasi disimpan.");
  await periksa(lampu.kasus, false, "SEBAGIAN", 1, "");
  await click("Simpan hasil pemeriksaan");
  await visible("Verifikasi disimpan.");

  await masuk("admin@example.invalid", "/admin");
  await open("/admin/kasus");
  await page.getByLabel("Status", { exact: true }).selectOption("MENUNGGU_PENUGASAN");
  await click("Terapkan");
  const barisPertama = page.locator("tbody tr").first();
  assert((await barisPertama.innerText()).includes("K-" + kabel.kasus), "Kasus kritis tidak berada di urutan pertama.");
  await visible("Kritis", "tbody");
  await visible("Tinggi", "tbody");
  await page.screenshot({ path: ".cache/browser/admin-kasus-desktop.png" });

  await open("/admin/kasus/" + pertama.kasus);
  assert(await page.locator("#petugasId option").filter({ hasText: "Raka Santoso" }).count() === 0, "Keahlian tidak cocok muncul dalam penugasan.");
  await tugaskan(pertama.kasus, "Dimas Pratama");
  await tugaskan(kabel.kasus, "Raka Santoso");
  await open("/admin/kasus/" + lampu.kasus);
  await visible("Belum ada petugas dengan keahlian ini yang tersedia sekarang.");
  assert(await page.locator("#petugasId").count() === 0, "Form penugasan muncul ketika semua petugas yang cocok sibuk.");

  await masuk("dimas@example.invalid", "/petugas");
  await open("/petugas/tugas");
  await page.getByRole("link", { name: "Buka dan mulai perbaikan" }).first().click();
  const pekerjaan = new URL(page.url()).pathname;
  await click("Mulai perbaikan");
  await visible("Perbaikan dimulai.");
  await page.getByLabel("Catatan penanganan").fill("QA: Suku cadang telah tiba.");
  await click("Simpan progres");
  await visible("Catatan progres disimpan.");
  assert((await page.getByLabel("Catatan penanganan").inputValue()) === "QA: Suku cadang telah tiba.", "Catatan progres tidak tersimpan.");
  await page.screenshot({ path: ".cache/browser/petugas-tugas-desktop.png" });
  await page.getByLabel("Catatan penanganan").fill("QA: Kompresor diganti. Pendinginan normal kembali.");
  page.once("dialog", dialog => dialog.accept());
  await click("Tandai selesai");
  await visible("Perbaikan selesai.");
  await visible("QA: Kompresor diganti.", ".p-panel-aksi");

  await masuk("admin@example.invalid", "/admin");
  await open("/admin/kasus/" + pertama.kasus);
  await visible("Selesai", ".a-kepala-status");
  await open("/admin/petugas");
  await page.locator("tbody tr").filter({ hasText: "Dimas Pratama" }).getByText("Tersedia", { exact: true }).waitFor();

  await masuk("andi@example.invalid", "/mahasiswa");
  await open(pertama.laporan);
  await visible("Tahap 5/5", ".m-detail-status");
  await visible("Pendinginan normal kembali.", ".sorot");
  const berulang = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: Kasus AC kembali muncul setelah pekerjaan selesai.");
  assert(berulang.kasus !== pertama.kasus && berulang.kasus !== hasilPisah, "Kejadian ulang masuk kasus lama.");

  for (const [size, pathname, screenshot] of [
    [{ width: 768, height: 1024 }, "/mahasiswa/laporan", "riwayat-tablet"],
    [{ width: 390, height: 844 }, "/mahasiswa", "mahasiswa-mobile"],
    [{ width: 390, height: 844 }, "/mahasiswa/laporan/baru", "form-mobile"],
    [{ width: 360, height: 800 }, "/mahasiswa/laporan", "riwayat-360"]
  ]) {
    await page.setViewportSize(size);
    await open(pathname);
    const cocok = await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth);
    assert(cocok, "Halaman meluber pada " + pathname + " dengan lebar " + size.width);
    await page.screenshot({ path: ".cache/browser/" + screenshot + ".png", fullPage: true });
  }
  const tidakAda = await page.goto(base + "/mahasiswa/laporan/999999");
  assert(tidakAda.status() === 404, "Laporan yang tidak ada harus mengembalikan 404.");
  await visible("Laporan tidak ditemukan");
  await keluar();
  return { laporan: pertama.laporan, kasus: pertama.kasus, pekerjaan, kasusBaru: berulang.kasus, kasusKritis: kabel.kasus, kursi: kursi.kasus };
}
