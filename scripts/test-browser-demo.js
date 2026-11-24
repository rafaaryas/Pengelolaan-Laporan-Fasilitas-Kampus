async page => {
  // Alur penuh lewat browser sesuai layar acuan:
  // civitas melapor (dengan foto) -> admin verifikasi + tugaskan -> petugas perbarui progres -> petugas tandai selesai (foto wajib)
  // -> civitas melihat timeline 4 tahap selesai. Jalankan terhadap skema yang memuat data demo db/demo (lihat docs/TEST_RESULTS.md).
  const base = globalThis.UMPEDULI_BASE || "http://127.0.0.1:8081";
  const foto = "src/main/resources/demo-unggahan/ac-ruang.png";
  const assert = (kondisi, pesan) => { if (!kondisi) throw new Error(pesan); };
  const open = async path => {
    const respons = await page.goto(base + path);
    assert(respons?.status() === 200, path + " mengembalikan " + respons?.status());
  };
  const visible = async (teks, selector = "main") => {
    await page.locator(selector).getByText(teks, { exact: false }).first().waitFor({ state: "visible" });
  };
  const angka = async selector => Number((await page.locator(selector).first().innerText()).trim());
  const keluar = async () => {
    await page.locator(".pengguna-tombol").click();
    await page.getByRole("button", { name: "Masuk dengan akun lain" }).click();
    await page.waitForURL(base + "/login");
  };
  const masuk = async (email, ruang) => {
    if (await page.locator(".pengguna-tombol").count()) await keluar();
    await open("/login");
    await page.getByLabel("Email kampus atau NIM").fill(email);
    await page.getByRole("button", { name: "Masuk", exact: true }).click();
    await page.waitForURL(base + ruang);
  };

  // 1. Civitas membuat laporan.
  await masuk("nadia@example.invalid", "/mahasiswa");
  const totalAwal = Number((await page.locator(".bagian-meta span").first().innerText()).trim());
  await open("/mahasiswa/laporan/baru");
  await page.getByRole("button", { name: "Kirim Laporan" }).click();
  await visible("Pilih gedung.");
  assert(await page.evaluate(() => document.activeElement.id) === "gedung", "Fokus tidak pindah ke galat pertama");
  await page.locator("#gedung").selectOption("B11");
  await page.locator("#lantai").selectOption("3");
  await page.locator("#ruang").fill("305");
  await page.locator("#deskripsi").fill("Lampu ruang 305 mati sejak pagi. Ruangan gelap saat kuliah sore.");
  await page.locator("#foto").setInputFiles(foto);
  await visible("Siap dikirim");
  await page.getByRole("button", { name: "Kirim Laporan" }).click();
  await page.waitForURL(/\/mahasiswa\/laporan\/\d+$/);
  await visible("Laporan terkirim", ".toast-wadah");
  await visible("Menunggu Verifikasi");
  const kode = (await page.locator(".kepala-hal p").innerText()).split(" ")[0];
  const urlLaporan = new URL(page.url()).pathname;
  await open("/mahasiswa");
  assert(Number((await page.locator(".bagian-meta span").first().innerText()).trim()) === totalAwal + 1, "Jumlah laporan terkirim tidak bertambah");

  // 2. Admin memverifikasi lewat modal dan menugaskan Budi Santoso.
  await masuk("admin@example.invalid", "/admin");
  const badgeAwal = await angka(".sisi .nav-badge");
  await open("/admin/kasus");
  await page.locator("#q").fill(kode);
  await page.locator(".tabel tbody tr").filter({ hasText: kode }).first().waitFor();
  await page.getByRole("link", { name: new RegExp(kode) }).click();
  const dialog = page.locator("dialog[open]");
  await dialog.getByText("Tindak lanjut laporan").waitFor();
  await page.keyboard.press("Escape");
  await dialog.waitFor({ state: "detached" });
  await page.getByRole("link", { name: new RegExp(kode) }).click();
  const budi = await dialog.locator("#petugas-verifikasi option", { hasText: "Budi Santoso" }).getAttribute("value");
  await dialog.locator("#petugas-verifikasi").selectOption(budi);
  await dialog.getByRole("button", { name: "Verifikasi laporan" }).click();
  await visible("Laporan terverifikasi dan petugas ditugaskan", ".toast-wadah");
  assert(await angka(".sisi .nav-badge") === badgeAwal - 1, "Badge Laporan tidak berkurang setelah verifikasi");

  // 3. Petugas memperbarui progres, lalu menandai selesai (foto wajib).
  await masuk("budi.santoso@example.invalid", "/petugas");
  await page.locator(".tugas-item").filter({ hasText: kode }).getByRole("link", { name: /Lihat tugas/ }).click();
  await page.locator("#catatan").fill("Starter lampu rusak, sedang diganti.");
  await page.getByRole("button", { name: "Simpan pembaruan" }).click();
  await visible("Pembaruan tersimpan", ".toast-wadah");
  await visible("Sedang Diperbaiki");
  await page.getByRole("button", { name: "Tandai selesai" }).click();
  await visible("Unggah foto hasil perbaikan sebelum menandai selesai.");
  await page.locator("#foto").setInputFiles(foto);
  await page.getByRole("button", { name: "Tandai selesai" }).click();
  await visible("Tugas ditandai selesai", ".toast-wadah");

  // 4. Civitas melihat timeline selesai.
  await masuk("nadia@example.invalid", "/mahasiswa");
  await open(urlLaporan);
  await visible("Perbaikan selesai");
  assert(await page.locator(".linimasa li.lewat").count() === 4, "Timeline belum terisi empat tahap");
  assert(await page.locator(".linimasa").getAttribute("data-tahap") === "4", "Tahap akhir bukan 4");

  // 5. Responsif: tidak ada scroll horizontal.
  for (const [ukuran, path] of [
    [{ width: 390, height: 844 }, "/mahasiswa"],
    [{ width: 390, height: 844 }, "/mahasiswa/laporan"],
    [{ width: 390, height: 844 }, "/mahasiswa/laporan/baru"],
    [{ width: 768, height: 1024 }, urlLaporan],
    [{ width: 1024, height: 900 }, "/mahasiswa"]
  ]) {
    await page.setViewportSize(ukuran);
    await open(path);
    const cocok = await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth);
    assert(cocok, "Halaman meluber pada " + path + " dengan lebar " + ukuran.width);
  }
  const tidakAda = await page.goto(base + "/mahasiswa/laporan/999999");
  assert(tidakAda.status() === 404, "Laporan yang tidak ada harus mengembalikan 404.");
  await open("/mahasiswa");
  await keluar();
  return { kode, laporan: urlLaporan };
}
