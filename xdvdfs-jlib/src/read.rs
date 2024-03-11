use crate::img::open_image;
use maybe_async::maybe_async;
use std::path::Path;

#[maybe_async]
pub async fn stat(img_path: &Path) -> Result<[i32; 2], anyhow::Error> {
    let mut img = open_image(img_path).await?;
    let volume = xdvdfs::read::read_volume(&mut img).await?;

    let tree = volume.root_table.file_tree(&mut img).await?;

    let mut total_size: i32 = 0;
    let mut file_count: i32 = 0;

    for (_dir, file) in &tree {
        if !file.node.dirent.is_directory() {
            total_size += file.node.dirent.data.size() as i32;
            file_count += 1;
        }
    }

    Ok([file_count, total_size])
}
