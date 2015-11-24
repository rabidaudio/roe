class AuthorSerializer < ActiveModel::Serializer
  attributes :id, :name, :email, :avatar, :created_at, :updated_at

  has_many :posts
end
