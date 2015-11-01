class CreatePosts < ActiveRecord::Migration
  def change
    create_table :posts do |t|
      t.string :title
      t.text :body
      t.references :author, index: true, foreign_key: true
      t.integer :likes, null: false, default: 0

      t.timestamps null: false
    end
  end
end
