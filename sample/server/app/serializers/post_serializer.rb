class PostSerializer < ActiveModel::Serializer
  attributes :id, :title, :body, :likes, :created_at, :updated_at

  belongs_to :author
end
