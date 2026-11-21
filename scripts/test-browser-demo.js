
async page => {
  const base = "http://127.0.0.1:8081";
  const assert = (condition, message) => {
    if (!condition) throw new Error(message);
  };
  const open = async path => {
    const response = await page.goto(base + path);
    assert(response?.status() === 200, path + " mengembalikan " + response?.status());
    assert(page.url() === base + path, "Pengalihan tak terduga dari " + path);
  };
  const visible = async (text, selector = "main") => {
    await page.locator(selector).getByText(text, { exact: false }).first().waitFor({ state: "visible" });
  };
  const click = async name => page.getByRole("button", { name, exact: true }).click();
  const akun = async (role, name) => {
    await open("/" + role.toLowerCase());
    await page.getByLabel("Identitas demo " + role).selectOption({ label: name });
    await click("Ganti");
    await visible("Selamat datang, " + name);
  };
  const laporan = async (fasilitas, jenis, uraian) => {
    await open("/mahasiswa/laporan/baru");
    const option = page.locator("#fasilitasId option").filter({ hasText: new RegExp("^" + fasilitas + " ·") });
    await page.locator("#fasilitasId").selectOption(await option.getAttribute("value"));
    assert((await page.locator("#lokasi").inputValue()).startsWith("Gedung"), "Lokasi fasilitas tidak diperbarui");
    await page.getByLabel("Jenis kerusakan").selectOption(jenis);
    await page.getByLabel("Deskripsi kerusakan").fill(uraian);
    await click("Kirim laporan");
    await visible("Laporan berhasil dikirim.");
    await visible("Menunggu verifikasi", ".badge-row");
    assert(await page.locator(".badge-row [class*='prioritas-']").count() === 0, "Prioritas muncul sebelum verifikasi");
    return {
      laporan: new URL(page.url()).pathname,
      kasus: (await page.locator("dd").filter({ hasText: /^K-\d+$/ }).innerText()).slice(2)
    };
  };
  const periksa = async (kasus, bahaya, gangguan, unit, catatan) => {
    await open("/petugas/kasus/" + kasus + "/verifikasi");
    await page.locator("#bahayaKritis").selectOption(String(bahaya));
    await page.locator("#gangguan").selectOption(gangguan);
    await page.locator("#unitRusak").fill(String(unit));
    await page.locator("#catatan").fill(catatan);
  };
  const tugaskan = async (kasus, nama) => {
    await open("/admin/kasus/" + kasus);
    await page.getByLabel("Petugas", { exact: false }).selectOption({ label: nama });
    await page.getByLabel("Catatan penugasan").fill("Periksa dan pulihkan fungsi fasilitas.");
    await click("Tugaskan petugas");
    await visible("Petugas berhasil ditugaskan.");
    await visible(nama, ".workspace aside");
  };

  await page.setViewportSize({ width: 1440, height: 1000 });
  await open("/");
  await open("/admin");
  await visible("Belum ada kasus");
  await open("/petugas");
  await visible("Belum ada tugas perbaikan");
  await open("/mahasiswa");
  await visible("Belum ada laporan");
  await page.screenshot({ path: ".cache/browser/mahasiswa-desktop.png" });
  await open("/mahasiswa/laporan/baru");
  await page.locator("form.form-stack").evaluate(form => { form.noValidate = true; });
  await click("Kirim laporan");
  await visible("Isi deskripsi kerusakan.");

  const pertama = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 1 tidak dingin.");
  await akun("Mahasiswa", "Budi");
  const budi = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 1 masih tidak dingin.");
  assert(pertama.kasus === budi.kasus, "Laporan Budi belum tergabung.");
  await akun("Mahasiswa", "Andi");
  const andi = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: AC 2 mati total.");
  assert(pertama.kasus === andi.kasus, "Laporan berulang seharusnya tergabung.");
  const berbeda = await laporan("AC B13", "KEBOCORAN", "QA: Pipa AC bocor.");
  assert(berbeda.kasus !== pertama.kasus, "Jenis kerusakan berbeda masuk kasus yang sama.");
  await open("/admin/kasus/" + pertama.kasus);
  await visible("3 laporan · 2 pelapor unik");
  assert(await page.getByRole("button", { name: "Tugaskan petugas" }).count() === 0, "Kasus belum diperiksa dapat ditugaskan.");
  page.once("dialog", dialog => dialog.accept());
  await page.locator(".aduan").filter({ hasText: "QA: AC 2 mati total." }).getByRole("button", { name: "Pisahkan laporan" }).click();
  await visible("Laporan dipindahkan ke kasus baru");
  const hasilPisah = new URL(page.url()).pathname.split("/").at(-1);
  assert(hasilPisah !== pertama.kasus, "Kasus koreksi sama dengan kasus asal.");
  await open(andi.laporan);
  await visible("K-" + hasilPisah, ".facts");

  await akun("Mahasiswa", "Sinta");
  const kabel = await laporan("Instalasi listrik", "BAHAYA_LISTRIK", "QA: Kabel terbuka di lantai 2.");
  const lampu = await laporan("Lampu ruang kelas", "TIDAK_BERFUNGSI", "QA: Satu lampu tidak menyala.");
  await akun("Petugas", "Dimas Pratama");
  await periksa(pertama.kasus, false, "BERAT", 10, "QA: Kompresor perlu diganti.");
  await page.locator("form.assignment").evaluate(form => { form.noValidate = true; });
  await click("Simpan verifikasi");
  await visible("jumlah unit rusak antara 1 dan 4", ".alert");
  await page.screenshot({ path: ".cache/browser/verifikasi-validasi.png" });
  await page.locator("#unitRusak").fill("1");
  await click("Simpan verifikasi");
  await visible("Verifikasi disimpan.");
  await akun("Petugas", "Raka Santoso");
  await periksa(kabel.kasus, true, "RINGAN", 1, "QA: Konduktor terbuka.");
  await click("Simpan verifikasi");
  await visible("Verifikasi disimpan.");
  await periksa(lampu.kasus, false, "SEBAGIAN", 1, "");
  await click("Simpan verifikasi");
  await visible("Verifikasi disimpan.");

  await open("/admin/kasus");
  await page.getByLabel("Status", { exact: true }).selectOption("MENUNGGU_PENUGASAN");
  await click("Terapkan");
  const criticalRow = page.locator("tbody tr").first();
  assert((await criticalRow.innerText()).includes("K-" + kabel.kasus), "Kasus kritis tidak berada di urutan pertama.");
  await visible("Kritis", "tbody");
  await visible("Tinggi", "tbody");
  await page.screenshot({ path: ".cache/browser/admin-kasus-desktop.png" });

  await open("/admin/kasus/" + pertama.kasus);
  assert(await page.locator("#petugasId option").filter({ hasText: "Raka Santoso" }).count() === 0, "Keahlian tidak cocok muncul dalam penugasan.");
  await tugaskan(pertama.kasus, "Dimas Pratama");
  await tugaskan(kabel.kasus, "Raka Santoso");
  await open("/admin/kasus/" + lampu.kasus);
  await visible("Belum ada petugas dengan keahlian yang sesuai dan sedang tersedia.");
  assert(await page.locator("#petugasId").count() === 0, "Form penugasan muncul ketika semua petugas yang cocok sibuk.");

  await akun("Petugas", "Dimas Pratama");
  await open("/petugas/tugas");
  await page.getByRole("link", { name: "Lihat tugas", exact: true }).click();
  const pekerjaan = new URL(page.url()).pathname;
  await visible("Ditugaskan", ".badge-row");
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
  await visible("QA: Kompresor diganti.", ".highlight");
  await visible("Diselesaikan", ".facts");
  await open("/admin/kasus/" + pertama.kasus);
  await visible("Selesai", ".badge-row, .section-heading");
  await open("/admin/petugas");
  await page.locator("tbody tr").filter({ hasText: "Dimas Pratama" }).getByText("Tersedia", { exact: true }).waitFor();

  await akun("Mahasiswa", "Andi");
  await open(pertama.laporan);
  await visible("Selesai", ".badge-row");
  await visible("Pendinginan normal kembali.", ".highlight");
  const berulang = await laporan("AC B13", "TIDAK_BERFUNGSI", "QA: Kasus AC kembali muncul setelah pekerjaan selesai.");
  assert(berulang.kasus !== pertama.kasus && berulang.kasus !== hasilPisah, "Kejadian ulang masuk kasus lama.");

  for (const [size, pathname, screenshot] of [
    [{ width: 768, height: 1024 }, "/admin/kasus", "admin-tablet"],
    [{ width: 390, height: 844 }, "/mahasiswa", "mahasiswa-mobile"],
    [{ width: 390, height: 844 }, "/mahasiswa/laporan/baru", "form-mobile"],
    [{ width: 390, height: 844 }, "/mahasiswa/laporan", "riwayat-mobile"],
    [{ width: 320, height: 800 }, "/admin/kasus", "admin-mobile"]
  ]) {
    await page.setViewportSize(size);
    await open(pathname);
    const cocok = await page.evaluate(() => document.documentElement.scrollWidth === window.innerWidth);
    assert(cocok, "Halaman meluber pada " + pathname + " dengan lebar " + size.width);
    await page.screenshot({ path: ".cache/browser/" + screenshot + ".png", fullPage: true });
  }

  const status404 = await page.goto(base + "/admin/kasus/999999");
  assert(status404.status() === 404, "Kasus yang tidak ada harus mengembalikan 404.");
  await visible("Kasus tidak ditemukan.");
  return { laporan: pertama.laporan, kasus: pertama.kasus, pekerjaan, kasusBaru: berulang.kasus, kasusKritis: kabel.kasus };
}
